// Copyright (c) 2018-2020, Tom Geiselmann
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.none.tom.exiferaser

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.none.tom.exiferaser.data.ImageRepository
import com.none.tom.exiferaser.data.SharedPrefsRepository
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import com.none.tom.exiferaser.ui.SharedViewModel
import com.none.tom.exiferaser.ui.SharedViewModelFactory
import kotlinx.coroutines.Dispatchers

class ExifEraserActivity : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels {
        applicationContext.let { context ->
            SharedViewModelFactory(
                ImageRepository(context, Dispatchers.IO),
                SharedPrefsRepository(context)
            )
        }
    }
    private lateinit var binding: ActivityExifEraserBinding
    private lateinit var listener: OnDestinationChangedListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExifEraserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupActionBar()
        navigateIfSendIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        navigateIfSendIntent()
    }

    override fun onDestroy() {
        super.onDestroy()
        findNavController(R.id.nav_controller).removeOnDestinationChangedListener(listener)
    }

    private fun setupActionBar() {
        binding.run {
            setSupportActionBar(toolbar)
            findNavController(R.id.nav_controller).let { navController ->
                NavigationUI.setupWithNavController(toolbar, navController)
                // Customizing the navigation icon currently is not possible,
                // see https://issuetracker.google.com/issues/121078028
                listener = OnDestinationChangedListener { controller, destination, _ ->
                    if (destination.id != controller.graph.startDestination) {
                        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
                    }
                }
                navController.addOnDestinationChangedListener(listener)
            }
        }
    }

    private fun navigateIfSendIntent() {
        intent.run {
            if (isMimeTypeUnsupported()) {
                showLongToast(R.string.mime_type_unsupported)
            } else if (isSupported()) {
                intent.toImageOrImagesSelection().let { selection ->
                    viewModel.selection = selection
                    with(findNavController(R.id.nav_controller)) {
                        handleDeepLink(
                            createDeepLink()
                                .setGraph(R.navigation.nav_graph)
                                .setDestination(R.id.fragment_images)
                                .createTaskStackBuilder()
                                .intents[0]
                        )
                    }
                }
            }
        }
    }
}
