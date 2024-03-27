/* Copyright 2024 Esri
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

package com.esri.arcgismaps.sample.editfeatureattributes.components

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.layers.ArcGISMapImageLayer
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import com.esri.arcgismaps.sample.editfeatureattributes.R
import com.esri.arcgismaps.sample.sampleslib.components.MessageDialogViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MapViewModel(
    application: Application,
    private val sampleCoroutineScope: CoroutineScope
) : AndroidViewModel(application) {
    // create a map using the topographic basemap style
    val map: ArcGISMap = ArcGISMap(BasemapStyle.ArcGISTopographic)

    // create a mapViewProxy that will be used to identify features in the MapView
    // should also be passed to the composable MapView this mapViewProxy is associated with
    val mapViewProxy = MapViewProxy()

    // create a ViewModel to handle dialog interactions
    val messageDialogVM: MessageDialogViewModel = MessageDialogViewModel()

    // string text to display the identify layer results
    val bottomTextBanner = mutableStateOf("Tap on the map to identify feature layers")


    init {
        // create a feature layer of damaged property data
        val featureTable = ServiceFeatureTable(application.getString(R.string.damage_assessment))
        val featureLayer = FeatureLayer.createWithFeatureTable(featureTable)

        // add the damaged properties feature layer
        map.apply {
            // set initial Viewpoint to North America
            initialViewpoint = Viewpoint(39.8, -98.6, 5e7)
            operationalLayers.add(featureLayer)
        }

    }
}
