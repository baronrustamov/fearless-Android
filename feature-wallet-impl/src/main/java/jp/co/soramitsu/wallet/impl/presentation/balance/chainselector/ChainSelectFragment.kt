package jp.co.soramitsu.wallet.impl.presentation.balance.chainselector

import android.content.DialogInterface
import android.widget.FrameLayout
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import jp.co.soramitsu.common.base.BaseComposeBottomSheetDialogFragment
import jp.co.soramitsu.common.compose.component.BottomSheetScreen
import jp.co.soramitsu.runtime.multiNetwork.chain.model.ChainId

@AndroidEntryPoint
class ChainSelectFragment : BaseComposeBottomSheetDialogFragment<ChainSelectViewModel>() {
    companion object {
        const val KEY_SELECTED_CHAIN_ID = "KEY_SELECTED_CHAIN_ID"
        const val KEY_SELECTED_ASSET_ID = "KEY_SELECTED_ASSET_ID"
        const val KEY_FILTER_CHAIN_IDS = "KEY_FILTER_CHAIN_IDS"
        const val KEY_CURRENCY_ID = "KEY_CURRENCY_ID"
        const val KEY_CHOOSER_MODE = "KEY_CHOOSER_MODE"
        const val KEY_SHOW_ALL_CHAINS = "KEY_SHOW_ALL_CHAINS"

        fun getBundle(assetId: String, chainId: ChainId? = null, chooserMode: Boolean = false) = bundleOf(
            KEY_SELECTED_ASSET_ID to assetId,
            KEY_SELECTED_CHAIN_ID to chainId,
            KEY_CHOOSER_MODE to chooserMode,
            KEY_SHOW_ALL_CHAINS to false
        )

        fun getBundle(
            selectedChainId: ChainId?,
            filterChainIds: List<ChainId>?,
            chooserMode: Boolean = true,
            currencyId: String?,
            showAllChains: Boolean = true
        ) = bundleOf(
            KEY_SELECTED_CHAIN_ID to selectedChainId,
            KEY_FILTER_CHAIN_IDS to filterChainIds,
            KEY_CHOOSER_MODE to chooserMode,
            KEY_CURRENCY_ID to currencyId,
            KEY_SHOW_ALL_CHAINS to showAllChains
        )
    }

    override val viewModel: ChainSelectViewModel by viewModels()

    @Composable
    override fun Content(padding: PaddingValues) {
        val state by viewModel.state.collectAsState()
        BottomSheetScreen {
            ChainSelectContent(
                state = state,
                onChainSelected = viewModel::onChainSelected,
                onSearchInput = viewModel::onSearchInput
            )
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        viewModel.onDialogClose()
    }

    override fun setupBehavior(behavior: BottomSheetBehavior<FrameLayout>) {
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.isHideable = true
        behavior.skipCollapsed = true
    }
}
