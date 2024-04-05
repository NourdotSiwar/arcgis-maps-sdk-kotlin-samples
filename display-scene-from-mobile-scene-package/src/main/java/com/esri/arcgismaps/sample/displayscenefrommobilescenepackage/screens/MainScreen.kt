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

package com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.screens

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.CameraController
import com.arcgismaps.mapping.view.GlobeCameraController
import com.arcgismaps.toolkit.geoviewcompose.SceneView
import com.arcgismaps.toolkit.geoviewcompose.SceneViewProxy
import com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.components.SceneViewModel
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialog
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar

/**
 * Main screen layout for the sample app
 */
@Composable
fun MainScreen(sampleName: String) {
    val sampleCoroutineScope = rememberCoroutineScope()
    val application = LocalContext.current.applicationContext as Application
    val ogcPath = LocalContext.current.getExternalFilesDir(null)?.path + "/Gravel_3d_mesh.json"
    val sceneViewModel = remember { SceneViewModel(application, sampleCoroutineScope, ogcPath) }

    val sceneViewProxy by remember { mutableStateOf( SceneViewProxy()) }
    val cameraController: CameraController by remember { mutableStateOf(GlobeCameraController()) }
    val camera = Camera(
        Point(8.021548309454653, 46.33754563822651, 945.7098963772878,
            SpatialReference.wgs84()), 31.998409907511512 , 68.28104607356202, 0.0)

    LaunchedEffect(cameraController) {
        sceneViewProxy.apply {
            setViewpointCamera(camera)
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
            ) {
                // composable function that wraps the SceneView
                SceneView(
                    modifier = Modifier.fillMaxSize(),
                    arcGISScene = sceneViewModel.scene,
                    graphicsOverlays = listOf(sceneViewModel.getGraphicsOverlay()),
                    sceneViewProxy = sceneViewProxy
                )
                // display a dialog if the sample encounters an error
                sceneViewModel.messageDialogVM.apply {
                    if (dialogStatus) {
                        MessageDialog(
                            title = messageTitle,
                            description = messageDescription,
                            onDismissRequest = ::dismissDialog
                        )
                    }
                }
            }
        }
    )
}
