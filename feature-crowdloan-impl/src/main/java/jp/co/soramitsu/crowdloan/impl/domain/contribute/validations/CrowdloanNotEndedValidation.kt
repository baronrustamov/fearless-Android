package jp.co.soramitsu.crowdloan.impl.domain.contribute.validations

import jp.co.soramitsu.common.validation.DefaultFailureLevel
import jp.co.soramitsu.common.validation.ValidationStatus
import jp.co.soramitsu.crowdloan.api.data.repository.CrowdloanRepository
import jp.co.soramitsu.crowdloan.impl.domain.common.leaseIndexFromBlock
import jp.co.soramitsu.runtime.repository.ChainStateRepository

class CrowdloanNotEndedValidation(
    private val chainStateRepository: ChainStateRepository,
    private val crowdloanRepository: CrowdloanRepository
) : ContributeValidation {

    override suspend fun validate(value: ContributeValidationPayload): ValidationStatus<ContributeValidationFailure> {
        val chainId = value.asset.token.configuration.chainId
        val currentBlock = chainStateRepository.currentBlock(chainId)

        val blocksPerLease = crowdloanRepository.blocksPerLeasePeriod(chainId)
        val leaseOffset = crowdloanRepository.leaseOffset(chainId)

        val currentLeaseIndex = leaseIndexFromBlock(currentBlock, blocksPerLease, leaseOffset)

        return when {
            currentBlock >= value.crowdloan.fundInfo.end -> crowdloanEndedFailure()
            currentLeaseIndex > value.crowdloan.fundInfo.firstSlot -> crowdloanEndedFailure()
            else -> ValidationStatus.Valid()
        }
    }

    private fun crowdloanEndedFailure(): ValidationStatus.NotValid<ContributeValidationFailure> =
        ValidationStatus.NotValid(DefaultFailureLevel.ERROR, ContributeValidationFailure.CrowdloanEnded)
}
