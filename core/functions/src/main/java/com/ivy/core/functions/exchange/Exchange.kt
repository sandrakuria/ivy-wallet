package com.ivy.core.functions.exchange

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.computations.option
import com.ivy.data.CurrencyCode
import com.ivy.data.ExchangeRates
import com.ivy.frp.Pure

suspend fun exchange(
    rates: ExchangeRates,
    baseCurrency: CurrencyCode,
    from: CurrencyCode,
    to: CurrencyCode,
    amount: Double,
) = option {
    if (from == to) return@option amount
    if (amount == 0.0) return@option 0.0

    val rate = findRate(
        rates = rates,
        from = from,
        to = to,
        baseCurrency = baseCurrency,
    ).bind()

    rate * amount
}

suspend fun findRate(
    rates: ExchangeRates,
    from: CurrencyCode,
    to: CurrencyCode,
    baseCurrency: CurrencyCode,
): Option<Double> = option {
    val fromCurrency = from.validateCurrency().bind()
    val toCurrency = to.validateCurrency().bind()

    if (fromCurrency == toCurrency) return@option 1.0

    when (baseCurrency) {
        fromCurrency -> {
            // exchange from base currency to other currency
            //w e need the rate from baseCurrency to toCurrency
            rates[toCurrency].validateRate().bind()
            //toAmount = fromAmount * rateFromTo
        }
        toCurrency -> {
            // exchange from other currency to base currency
            // we'll get the rate to

            /*
            Example: fromA = 10 fromC = EUR; toC = BGN
            rateToFrom = rate (BGN EUR) ~= 0.51

            Formula: (10 EUR / 0.51 ~= 19.67)
                fromAmount / rateToFrom

            EXPECTED: 10 EUR ~= 19.67 BGN
             */
            1.0 / rates[fromCurrency].validateRate().bind()
        }
        else -> {
            //exchange from other currency to other currency
            //that's the only possible case left because we already checked "fromCurrency == toCurrency"

            val rateBaseFrom = rates[fromCurrency].validateRate().bind()
            val rateBaseTo = rates[toCurrency].validateRate().bind()

            //Convert: toBaseCurrency -> toToCurrency
            val rateBase = 1 / rateBaseFrom
            rateBase * rateBaseTo
        }
    }
}

@Pure
private fun String.validateCurrency(): Option<String> {
    return if (this.isNotBlank()) return Some(this) else None
}

@Pure
fun Double?.validateRate(): Option<Double> {
    val rate = this ?: return None
    //exchange rate which <= 0 is invalid!
    return if (rate > 0) return Some(this) else None
}