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

package com.esri.arcgismaps.sample.displayscenefrommobilescenepackage.components

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.layers.Ogc3DTilesLayer
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SurfacePlacement
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SceneViewModel(
    private val application: Application,
    private val sampleCoroutineScope: CoroutineScope,
    private val path: String
) : AndroidViewModel(application) {
    // create a base scene to be used to load the mobile scene package
    var scene by mutableStateOf(ArcGISScene())

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    init {
        addSurface()
        loadOgc()
    }

    private fun loadOgc() {
        val ogc3DTilesLayer = Ogc3DTilesLayer(path)
        Log.d("MainScreen", path)
        sampleCoroutineScope.launch {
            ogc3DTilesLayer.load().onSuccess {
                Log.d("MainScreen", ogc3DTilesLayer.loadStatus.value.toString())
                scene.operationalLayers.add(ogc3DTilesLayer)
            }.onFailure {
                messageDialogVM.showMessageDialog(it.message.toString(), it.cause.toString())
            }
        }
    }


    private fun addSurface() {
        scene.baseSurface.elevationSources.add(
            ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
        )
    }

    fun getGraphicsOverlay(): GraphicsOverlay {
        // adds three graphics to the scene view to verify the 3D tiles layer is placed correctly.
        val graphicsOverlay = GraphicsOverlay().apply {
            sceneProperties.surfacePlacement = SurfacePlacement.Absolute
        }
        val road1 = Point(8.02385, 46.3411, 712.613, SpatialReference.wgs84())
        val road2 = Point(8.02567, 46.3429, 718.523, SpatialReference.wgs84())
        val railroad1 = Point(8.02542, 46.3407, 712.205, SpatialReference.wgs84())
        val simpleMarkerSceneSymbol = SimpleMarkerSceneSymbol(
            SimpleMarkerSceneSymbolStyle.Diamond,
            Color.green,
            10.0,
            10.0,
            10.0,
            SceneSymbolAnchorPosition.Center
        )
        with(graphicsOverlay.graphics) {
            add(Graphic(road1, simpleMarkerSceneSymbol))
            add(Graphic(road2, simpleMarkerSceneSymbol))
            add(Graphic(railroad1, simpleMarkerSceneSymbol))
        }
        return graphicsOverlay
    }
}
