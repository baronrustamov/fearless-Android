package jp.co.soramitsu.staking.impl.domain.model

import java.math.BigInteger
import java.util.Calendar
import java.util.TimeZone
import jp.co.soramitsu.common.data.network.subquery.StakingHistoryRemote
import jp.co.soramitsu.common.utils.orZero
import jp.co.soramitsu.staking.api.domain.model.DelegationAction
import jp.co.soramitsu.staking.api.domain.model.DelegationScheduledRequest

data class Unbonding(
    val amount: BigInteger,
    val timeLeft: Long,
    val calculatedAt: Long,
    val type: DelegationAction?
)

fun DelegationScheduledRequest.toUnbonding(timeLeft: Long) = Unbonding(
    amount = this.actionValue,
    timeLeft = timeLeft,
    calculatedAt = System.currentTimeMillis(),
    type = action
)

fun StakingHistoryRemote.HistoryElement.toUnbonding(): Unbonding {
    val timeLeft = this.timestamp?.toLongOrNull()?.let {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis / 1000 - it
    } ?: 0

    return Unbonding(
        amount = this.amount?.toBigInteger().orZero(),
        timeLeft = timeLeft,
        calculatedAt = System.currentTimeMillis(),
        type = DelegationAction.byId(type?.toInt())
    )
}
