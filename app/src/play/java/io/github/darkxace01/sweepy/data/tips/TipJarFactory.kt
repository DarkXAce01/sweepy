package io.github.darkxace01.sweepy.data.tips

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

fun createTipJar(context: Context, scope: CoroutineScope, onTipped: (Tip, Int) -> Unit): TipJar = PlayTipJar(context, scope, onTipped)

/**
 * Tips through Google Play billing. Talks to the Play Store app on the phone, so Sweepy itself
 * still needs no internet permission. Tips are consumed right away: nothing to unlock or restore.
 */
private class PlayTipJar(context: Context, private val scope: CoroutineScope, private val onTipped: (Tip, Int) -> Unit) : TipJar {
    override val supported = true

    private val _state = MutableStateFlow<TipJarState>(TipJarState.Loading)
    override val state: StateFlow<TipJarState> = _state.asStateFlow()

    private val _outcomes = Channel<TipOutcome>(Channel.BUFFERED)
    override val outcomes: Flow<TipOutcome> = _outcomes.receiveAsFlow()

    private var details: Map<Tip, ProductDetails> = emptyMap()

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { result, purchases -> onPurchasesUpdated(result, purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    override fun refresh() {
        if (client.isReady) {
            loadProducts()
            finishOpenPurchases()
            return
        }
        if (_state.value !is TipJarState.Ready) _state.value = TipJarState.Loading
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    loadProducts()
                    finishOpenPurchases()
                } else {
                    _state.value = TipJarState.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() = Unit // Reconnects by itself (enableAutoServiceReconnection).
        })
    }

    private fun loadProducts() {
        val products = Tip.entries.map {
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(it.productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(products).build()) { result, found ->
            val byId = found.productDetailsList.associateBy { it.productId }
            details = Tip.entries.mapNotNull { tip -> byId[tip.productId]?.let { tip to it } }.toMap()
            val prices = details.mapNotNull { (tip, d) -> d.oneTimePurchaseOfferDetails?.formattedPrice?.let { tip to it } }.toMap()
            _state.value = if (result.responseCode == BillingResponseCode.OK && prices.isNotEmpty()) {
                TipJarState.Ready(prices)
            } else {
                TipJarState.Unavailable
            }
        }
    }

    override fun buy(activity: Activity, tip: Tip) {
        val product = details[tip]
        if (product == null) {
            scope.launch { _outcomes.send(TipOutcome.Failed) }
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()))
            .build()
        client.launchBillingFlow(activity, params)
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingResponseCode.OK -> purchases.orEmpty().forEach(::handle)
            BillingResponseCode.USER_CANCELED -> Unit
            // Already paid but not yet consumed (e.g. the app closed mid-way): finish that one.
            BillingResponseCode.ITEM_ALREADY_OWNED -> finishOpenPurchases()
            else -> scope.launch { _outcomes.send(TipOutcome.Failed) }
        }
    }

    /** Tips that were paid while Sweepy wasn't listening (slow payment methods, a crash mid-purchase). */
    private fun finishOpenPurchases() {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingResponseCode.OK) purchases.forEach(::handle)
        }
    }

    private fun handle(purchase: Purchase) {
        val tip = Tip.entries.firstOrNull { it.productId in purchase.products } ?: return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                // Consuming also acknowledges it; without that, Play refunds it after three days.
                val params = ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
                client.consumeAsync(params) { consumed, _ ->
                    if (consumed.responseCode == BillingResponseCode.OK) {
                        // With multi-quantity on in the Play Console, people can buy several at once.
                        val count = purchase.quantity.coerceAtLeast(1)
                        onTipped(tip, count)
                        scope.launch { _outcomes.send(TipOutcome.Thanked(tip, count)) }
                    }
                }
            }
            Purchase.PurchaseState.PENDING -> scope.launch { _outcomes.send(TipOutcome.Pending) }
            else -> Unit
        }
    }
}
