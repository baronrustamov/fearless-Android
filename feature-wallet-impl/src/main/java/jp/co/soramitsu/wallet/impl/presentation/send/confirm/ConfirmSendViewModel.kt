package jp.co.soramitsu.wallet.impl.presentation.send.confirm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import jp.co.soramitsu.account.api.presentation.actions.ExternalAccountActions
import jp.co.soramitsu.common.AlertViewState
import jp.co.soramitsu.common.address.AddressIconGenerator
import jp.co.soramitsu.common.address.AddressModel
import jp.co.soramitsu.common.address.createAddressModel
import jp.co.soramitsu.common.base.BaseViewModel
import jp.co.soramitsu.common.compose.component.ButtonViewState
import jp.co.soramitsu.common.compose.component.TitleValueViewState
import jp.co.soramitsu.common.data.network.BlockExplorerUrlBuilder
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.combine
import jp.co.soramitsu.common.utils.flowOf
import jp.co.soramitsu.common.utils.requireException
import jp.co.soramitsu.feature_wallet_impl.R
import jp.co.soramitsu.runtime.ext.utilityAsset
import jp.co.soramitsu.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.runtime.multiNetwork.chain.model.getSupportedExplorers
import jp.co.soramitsu.wallet.api.presentation.mixin.TransferValidityChecks
import jp.co.soramitsu.wallet.impl.data.mappers.mapAssetToAssetModel
import jp.co.soramitsu.wallet.impl.domain.CurrentAccountAddressUseCase
import jp.co.soramitsu.wallet.impl.domain.interfaces.NotValidTransferStatus
import jp.co.soramitsu.wallet.impl.domain.interfaces.WalletConstants
import jp.co.soramitsu.wallet.impl.domain.interfaces.WalletInteractor
import jp.co.soramitsu.wallet.impl.domain.model.PhishingType
import jp.co.soramitsu.wallet.impl.domain.model.Transfer
import jp.co.soramitsu.wallet.impl.domain.model.TransferValidityLevel
import jp.co.soramitsu.wallet.impl.domain.model.TransferValidityStatus
import jp.co.soramitsu.wallet.impl.domain.model.planksFromAmount
import jp.co.soramitsu.wallet.impl.presentation.WalletRouter
import jp.co.soramitsu.wallet.impl.presentation.send.TransferDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val ICON_IN_DP = 24

@HiltViewModel
class ConfirmSendViewModel @Inject constructor(
    private val interactor: WalletInteractor,
    private val router: WalletRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry,
    private val externalAccountActions: ExternalAccountActions.Presentation,
    private val walletConstants: WalletConstants,
    private val transferValidityChecks: TransferValidityChecks.Presentation,
    private val savedStateHandle: SavedStateHandle,
    private val resourceManager: ResourceManager,
    private val currentAccountAddress: CurrentAccountAddressUseCase
) : BaseViewModel(),
    ExternalAccountActions by externalAccountActions,
    TransferValidityChecks by transferValidityChecks,
    ConfirmSendScreenInterface {

    private val transferDraft = savedStateHandle.get<TransferDraft>(ConfirmSendFragment.KEY_DRAFT) ?: error("Required data not provided for send confirmation")
    private val phishingType = savedStateHandle.get<PhishingType>(ConfirmSendFragment.KEY_PHISHING_TYPE)

    private val recipientFlow = interactor.observeAddressBook(transferDraft.assetPayload.chainId).map { contacts ->
        val contactName = contacts.firstOrNull { it.address.equals(transferDraft.recipientAddress, ignoreCase = true) }?.name
        getAddressModel(transferDraft.recipientAddress, contactName)
    }

    private val senderFlow = flowOf {
        currentAccountAddress(transferDraft.assetPayload.chainId)?.let { address ->
            val walletName = interactor.getSelectedMetaAccount().name
            getAddressModel(address, walletName)
        }
    }

    private val transferSubmittingFlow = MutableStateFlow(false)

    private val defaultButtonState = ButtonViewState(
        resourceManager.getString(R.string.common_confirm),
        true
    )

    private val buttonStateFlow = transferSubmittingFlow.map { submitting ->
        ButtonViewState(
            text = resourceManager.getString(R.string.common_confirm),
            enabled = !submitting
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, defaultButtonState)

    private val assetFlow = interactor.assetFlow(transferDraft.assetPayload.chainId, transferDraft.assetPayload.chainAssetId)
        .map(::mapAssetToAssetModel)

    @OptIn(ExperimentalCoroutinesApi::class)
    val utilityAssetFlow = flowOf {
        val assetChain = interactor.getChain(transferDraft.assetPayload.chainId)
        assetChain.utilityAsset.id
    }.flatMapLatest { assetId ->
        interactor.assetFlow(transferDraft.assetPayload.chainId, assetId)
            .map(::mapAssetToAssetModel)
    }

    val state: StateFlow<ConfirmSendViewState> = combine(
        recipientFlow,
        senderFlow,
        assetFlow,
        utilityAssetFlow,
        buttonStateFlow,
        transferSubmittingFlow
    ) { recipient, sender, asset, utilityAsset, buttonState, isSubmitting ->
        val isSenderNameSpecified = !sender?.name.isNullOrEmpty()
        val fromInfoItem = TitleValueViewState(
            title = resourceManager.getString(R.string.transaction_details_from),
            value = if (isSenderNameSpecified) sender?.name else sender?.address?.shorten(),
            additionalValue = if (isSenderNameSpecified) sender?.address?.shorten() else null
        )

        val isRecipientNameSpecified = !recipient.name.isNullOrEmpty()
        val toInfoItem = TitleValueViewState(
            title = resourceManager.getString(R.string.choose_amount_to),
            value = if (isRecipientNameSpecified) recipient.name else recipient.address.shorten(),
            additionalValue = if (isRecipientNameSpecified) recipient.address.shorten() else null,
            clickState = phishingType?.let { TitleValueViewState.ClickState(R.drawable.ic_alert_16, ConfirmSendViewState.CODE_WARNING_CLICK) }
        )

        val amountInfoItem = TitleValueViewState(
            title = resourceManager.getString(R.string.common_amount),
            value = asset.formatTokenAmount(transferDraft.amount),
            additionalValue = asset.getAsFiatWithCurrency(transferDraft.amount)
        )

        val tipInfoItem = transferDraft.tip?.let {
            TitleValueViewState(
                title = resourceManager.getString(R.string.choose_amount_tip),
                value = utilityAsset.formatTokenAmount(transferDraft.tip),
                additionalValue = utilityAsset.getAsFiatWithCurrency(transferDraft.tip)
            )
        }

        val feeInfoItem = TitleValueViewState(
            title = resourceManager.getString(R.string.network_fee),
            value = utilityAsset.formatTokenAmount(transferDraft.fee),
            additionalValue = utilityAsset.getAsFiatWithCurrency(transferDraft.fee)
        )

        ConfirmSendViewState(
            chainIconUrl = asset.token.configuration.chainIcon ?: asset.token.configuration.iconUrl,
            fromInfoItem = fromInfoItem,
            toInfoItem = toInfoItem,
            amountInfoItem = amountInfoItem,
            tipInfoItem = tipInfoItem,
            feeInfoItem = feeInfoItem,
            buttonState = buttonState,
            isLoading = isSubmitting
        )
    }.stateIn(this, SharingStarted.Eagerly, ConfirmSendViewState.default)

    override fun onNavigationClick() {
        router.back()
    }

    override fun copyRecipientAddressClicked() {
        launch {
            val chainId = transferDraft.assetPayload.chainId
            val chain = chainRegistry.getChain(chainId)
            val supportedExplorers = chain.explorers.getSupportedExplorers(BlockExplorerUrlBuilder.Type.ACCOUNT, transferDraft.recipientAddress)
            val externalActionsPayload = ExternalAccountActions.Payload(
                value = transferDraft.recipientAddress,
                chainId = chainId,
                chainName = chain.name,
                explorers = supportedExplorers
            )

            externalAccountActions.showExternalActions(externalActionsPayload)
        }
    }

    override fun onNextClick() {
        performTransfer(suppressWarnings = false)
    }

    override fun onItemClick(code: Int) {
        when (code) {
            ConfirmSendViewState.CODE_WARNING_CLICK -> openWarningAlert()
        }
    }

    private fun openWarningAlert() {
        launch {
            val symbol = assetFlow.first().token.configuration.symbolToShow

            val payload = AlertViewState(
                title = getPhishingTitle(phishingType),
                message = getPhishingMessage(phishingType, symbol),
                buttonText = resourceManager.getString(R.string.top_up),
                iconRes = R.drawable.ic_alert_16
            )
            router.openAlert(payload)
        }
    }

    private fun getPhishingTitle(phishingType: PhishingType?): String {
        return when (phishingType) {
            PhishingType.SCAM -> resourceManager.getString(R.string.scam_alert_title)
            PhishingType.EXCHANGE -> resourceManager.getString(R.string.exchange_alert_title)
            PhishingType.DONATION -> resourceManager.getString(R.string.donation_alert_title)
            PhishingType.SANCTIONS -> resourceManager.getString(R.string.sanction_alert_title)
            else -> resourceManager.getString(R.string.donation_alert_title)
        }
    }

    private fun getPhishingMessage(phishingType: PhishingType?, symbol: String): String {
        return when (phishingType) {
            PhishingType.EXCHANGE -> resourceManager.getString(R.string.exchange_alert_message)
            else -> resourceManager.getString(R.string.scam_alert_message_format, symbol)
        }
    }

    fun warningConfirmed() {
        performTransfer(suppressWarnings = true)
    }

    fun errorAcknowledged() {
        router.back()
    }

    private fun performTransfer(suppressWarnings: Boolean) {
        launch {
            val token = assetFlow.firstOrNull()?.token?.configuration ?: return@launch

            transferSubmittingFlow.value = true

            val tipInPlanks = transferDraft.tip?.let { token.planksFromAmount(it) }
            val result = withContext(Dispatchers.Default) {
                interactor.performTransfer(createTransfer(token), transferDraft.fee, tipInPlanks)
            }
            if (result.isSuccess) {
                val operationHash = result.getOrNull()
                router.finishSendFlow()
                router.openOperationSuccess(operationHash, token.chainId)
            } else {
                val error = result.requireException()

                if (error is NotValidTransferStatus) {
                    processInvalidStatus(error.status)
                } else {
                    showError(error)
                }
            }

            transferSubmittingFlow.value = false
        }
    }

    private fun processInvalidStatus(status: TransferValidityStatus) {
        when (status) {
            is TransferValidityLevel.Warning.Status -> transferValidityChecks.showTransferWarning(status)
            is TransferValidityLevel.Error.Status -> transferValidityChecks.showTransferError(status)
        }
    }

    private suspend fun getAddressModel(address: String, accountName: String? = null): AddressModel {
        return addressIconGenerator.createAddressModel(address, ICON_IN_DP, accountName)
    }

    private fun createTransfer(token: Chain.Asset): Transfer {
        return with(transferDraft) {
            Transfer(
                recipient = recipientAddress,
                amount = amount,
                chainAsset = token
            )
        }
    }
}

private fun String.shorten() = when {
    length < 20 -> this
    else -> "${take(5)}...${takeLast(5)}"
}
