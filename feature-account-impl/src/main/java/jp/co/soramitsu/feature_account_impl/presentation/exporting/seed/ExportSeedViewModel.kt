package jp.co.soramitsu.feature_account_impl.presentation.exporting.seed

import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.common.utils.map
import jp.co.soramitsu.common.utils.requireHexPrefix
import jp.co.soramitsu.common.utils.requirePrefix
import jp.co.soramitsu.common.utils.toHex
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountInteractor
import jp.co.soramitsu.feature_account_api.domain.model.WithDerivationPath
import jp.co.soramitsu.feature_account_api.domain.model.WithSeed
import jp.co.soramitsu.feature_account_impl.presentation.AccountRouter
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportSource
import jp.co.soramitsu.feature_account_impl.presentation.exporting.ExportViewModel

class ExportSeedViewModel(
    private val router: AccountRouter,
    resourceManager: ResourceManager,
    accountInteractor: AccountInteractor,
    accountAddress: String
) : ExportViewModel(accountInteractor, accountAddress, resourceManager, ExportSource.Seed) {

    val seedLiveData = securityTypeLiveData.map {
        (it as WithSeed).seed!!.toHex().requireHexPrefix()
    }

    val derivationPath = securityTypeLiveData.map {
        (it as? WithDerivationPath)?.derivationPath
    }

    fun back() {
        router.back()
    }

    fun exportClicked() {
        showSecurityWarning()
    }

    override fun securityWarningConfirmed() {
        val seed = seedLiveData.value ?: return

        exportText(seed)
    }
}