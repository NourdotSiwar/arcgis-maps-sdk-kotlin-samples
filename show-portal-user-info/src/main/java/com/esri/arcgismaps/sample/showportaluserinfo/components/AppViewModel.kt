/*
 *
 *  Copyright 2023 Esri
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.esri.arcgismaps.sample.showportaluserinfo.components

import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.httpcore.authentication.OAuthUserConfiguration
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.authentication.AuthenticatorState
import com.arcgismaps.toolkit.authentication.signOut
import com.esri.arcgismaps.sample.showportaluserinfo.R
import kotlinx.coroutines.launch

class AppViewModel(private val application: Application) : AndroidViewModel(application) {

    val authenticatorState: AuthenticatorState = AuthenticatorState()
    private val oAuthUserConfiguration = OAuthUserConfiguration(
        "https://www.arcgis.com",
        // This client ID is for sample purposes only. For use of the Authenticator in your own app,
        // create your own client ID. For more info see: https://developers.arcgis.com/documentation/mapping-apis-and-services/security/tutorials/register-your-application/
        application.getString(R.string.oauth_client_id),
        application.getString(R.string.oauth_redirect_uri)
    )

    private val portalUserName = mutableStateOf("")

    private val emailID = mutableStateOf("")


    fun signOut() = viewModelScope.launch {
        ArcGISEnvironment.authenticationManager.signOut()

        emailID.value = ""
        portalUserName.value = ""
    }

    fun loadPortal() = viewModelScope.launch {
        authenticatorState.oAuthUserConfiguration = oAuthUserConfiguration
        val portal = Portal(oAuthUserConfiguration.portalUrl, Portal.Connection.Authenticated)
        portal.load().onFailure {
            // Handle portal login failure
            Toast.makeText(application.baseContext, "FAILED TO LOAD", Toast.LENGTH_SHORT).show()

        }.onSuccess {
            portal.portalInfo?.apply {
                portalUserName.value = this.user?.fullName.toString()
                emailID.value = this.user?.email.toString()
            }

            // Display result
            Toast.makeText(
                application.baseContext,
                "User: ${portalUserName.value}",
                Toast.LENGTH_SHORT
            ).show()

        }
    }
}
