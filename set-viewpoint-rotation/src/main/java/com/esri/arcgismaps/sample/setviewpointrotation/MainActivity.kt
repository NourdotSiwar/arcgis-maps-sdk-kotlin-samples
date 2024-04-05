/* Copyright 2022 Esri
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

package com.esri.arcgismaps.sample.setviewpointrotation

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.arcgismaps.ApiKey
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.Color
import com.arcgismaps.geometry.Point
import com.arcgismaps.geometry.SpatialReference
import com.arcgismaps.mapping.ArcGISScene
import com.arcgismaps.mapping.ArcGISTiledElevationSource
import com.arcgismaps.mapping.layers.Ogc3DTilesLayer
import com.arcgismaps.mapping.symbology.SceneSymbolAnchorPosition
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbol
import com.arcgismaps.mapping.symbology.SimpleMarkerSceneSymbolStyle
import com.arcgismaps.mapping.view.Camera
import com.arcgismaps.mapping.view.Graphic
import com.arcgismaps.mapping.view.GraphicsOverlay
import com.arcgismaps.mapping.view.SceneView
import com.arcgismaps.mapping.view.SurfacePlacement
import com.esri.arcgismaps.sample.setviewpointrotation.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var sceneView: SceneView
    private lateinit var scene: ArcGISScene

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // authentication with an API key or named user is
        // required to access basemaps and other location services
        ArcGISEnvironment.apiKey = ApiKey.create(BuildConfig.API_KEY)

        // set up data binding for the activity
        val activityMainBinding: ActivityMainBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_main)
        sceneView = activityMainBinding.sceneView
        lifecycle.addObserver(sceneView)

        // create a map with a topographic basemap and initial position
        scene = ArcGISScene().apply {
            baseSurface.elevationSources.add(
                ArcGISTiledElevationSource("https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer")
            )
        }
        onLayerViewStateChanged()
        addOgc()
        addGraphics()
        setCamera()
        sceneView.scene = scene
    }

    private fun onLayerViewStateChanged() {
        lifecycleScope.launch {
            sceneView.layerViewStateChanged.collect {
                Log.d("LayerViewStateChanged", it.layerViewState.status.toString())
            }
        }
    }

    private fun addOgc() {
        val tiles3dPath = getExternalFilesDir(null)?.path + "/Gravel_3d_mesh.json"
        val ogc3DTilesLayer = Ogc3DTilesLayer(tiles3dPath)
        scene.operationalLayers.add(ogc3DTilesLayer)
    }

    private fun addGraphics() {
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
        sceneView.graphicsOverlays.add(graphicsOverlay)
    }

    private fun setCamera() {
        // the camera is modified to fit the Android device view.
        val camera = Camera(
            Point(
                8.021548309454653, 46.33754563822651, 945.7098963772878,
                SpatialReference.wgs84()
            ), 31.998409907511512, 68.28104607356202, 0.0
        )
        sceneView.setViewpointCamera(camera)
    }
}
