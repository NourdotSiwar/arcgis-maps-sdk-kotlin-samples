/* Copyright 2023 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.esri.arcgismaps.sample.showportaluserinfo.screens

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.DialogAuthenticator
import com.esri.arcgismaps.sample.showportaluserinfo.components.AppViewModel

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String, application: Application) {

    val appViewModel = viewModel { AppViewModel(application) }
    val authenticatorState: AuthenticatorState = appViewModel.authenticatorState

    Surface(modifier = Modifier.fillMaxSize()) {
        // sign out if previous login is cached
        appViewModel.signOut()
        // load the secured portal
        appViewModel.loadPortal()
        DialogAuthenticator(authenticatorState = authenticatorState)
    }
}
