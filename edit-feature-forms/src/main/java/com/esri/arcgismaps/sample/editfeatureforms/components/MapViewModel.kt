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

package com.esri.arcgismaps.sample.editfeatureforms.components

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arcgismaps.data.ArcGISFeature
import com.arcgismaps.data.ServiceFeatureTable
import com.arcgismaps.exceptions.FeatureFormValidationException
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.Viewpoint
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.mapping.featureforms.FieldFormElement
import com.arcgismaps.mapping.featureforms.FormElement
import com.arcgismaps.mapping.featureforms.GroupFormElement
import com.arcgismaps.mapping.layers.FeatureLayer
import com.arcgismaps.mapping.view.SingleTapConfirmedEvent
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.geoviewcompose.MapViewProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout


/**
 * A UI state class that indicates the current editing state for a feature form.
 */
sealed class UIState {
    data object Loading: UIState()
    data object NotEditing: UIState()
    data class Editing(
        val featureForm: FeatureForm,
        val validationErrorVisibility: ValidationErrorVisibility = ValidationErrorVisibility.Automatic
    ) : UIState()
    data class Switching(
        val oldState:Editing,
        val newFeature: ArcGISFeature
    ): UIState()
    data class Committing(
        val featureForm: FeatureForm,
        val errors: List<ErrorInfo>
    ): UIState()
}

data class ErrorInfo(val fieldName: String, val error: FeatureFormValidationException)

class MapViewModel(
    private val coroutineScope: CoroutineScope,
    application: Application
) : AndroidViewModel(application) {
    val arcGISMap = 
        ArcGISMap(PortalItem(Portal("https://www.arcgis.com/"), "f72207ac170a40d8992b7a3507b44fad"))
    
    val mapViewProxy = MapViewProxy() 
    val uiState = mutableStateOf<UIState>(UIState.Loading)

    suspend fun commitEdits(): Result<Unit> {
        val state = (uiState.value as? UIState.Editing)
            ?: return Result.failure(IllegalStateException("Not in editing state"))
        // build the list of errors
        val errors = mutableListOf<ErrorInfo>()
        val featureForm = state.featureForm
        featureForm.validationErrors.value.forEach { entry ->
            entry.value.forEach { error ->
                featureForm.elements.getFormElement(entry.key)?.let { formElement ->
                    if (formElement.isEditable.value) {
                        errors.add(
                            ErrorInfo(
                                formElement.label,
                                error as FeatureFormValidationException
                            )
                        )
                    }
                }
            }
        }
        // set the state to committing with the errors if any
        uiState.value = UIState.Committing(
            featureForm = state.featureForm,
            errors = errors
        )
        // if there are no errors then update the feature
        return if (errors.isEmpty()) {
            val feature = state.featureForm.feature
            val serviceFeatureTable =
                feature.featureTable as? ServiceFeatureTable ?: return Result.failure(
                    IllegalStateException("cannot save feature edit without a ServiceFeatureTable")
                )
            Log.d("MapViewModel", "before serviceFeatureTable.updateFeature(feature)")
//            try {
//                withTimeout(5000) {
//                    serviceFeatureTable.updateFeature(feature)
//                }
//                Log.d("MapViewModel", "after serviceFeatureTable.updateFeature(feature)")
//            }catch (e: TimeoutCancellationException) {
//                Log.d("MapViewModel", "Function timed out")
//            }

            val res = serviceFeatureTable.updateFeature(feature).onSuccess {
                Log.d("MapViewModel", "after serviceFeatureTable.updateFeature(feature) onSuccess")
            }.onFailure {
                Log.d("MapViewModel", "after serviceFeatureTable.updateFeature(feature) onFailure")
            }
            res.getOrThrow()
            Log.d("MapViewModel", "after serviceFeatureTable.updateFeature(feature) res")
            val result = res.map {
                val db = serviceFeatureTable.serviceGeodatabase
                if (db != null && db.serviceInfo?.canUseServiceGeodatabaseApplyEdits == true) {
                    db.applyEdits()
                }
                else if (db !=null) {
                    serviceFeatureTable.applyEdits()
                }
                feature.refresh()
                // unselect the feature after the edits have been saved
                (feature.featureTable?.layer as FeatureLayer).clearSelection()
            }
            // set the state to not editing since the feature was updated successfully
            uiState.value = UIState.NotEditing
            result
        } else {
            // even though there are errors send a success result since the operation was successful
            // and the control is back with the UI
            Log.d("MapViewModel", "errors is not empty: $errors")

            Result.success(Unit)
        }
    }

    fun cancelCommit(): Result<Unit> {
        val previousState = (uiState.value as? UIState.Committing) ?: return Result.failure(
            IllegalStateException("Not in committing state")
        )
        // set the state back to an editing state while showing all errors using
        // ValidationErrorVisibility.Always
        uiState.value = UIState.Editing(
            previousState.featureForm,
            validationErrorVisibility = ValidationErrorVisibility.Visible
        )
        return Result.success(Unit)
    }

    fun selectNewFeature() =
        (uiState.value as? UIState.Switching)?.let { prevState ->
            prevState.oldState.featureForm.discardEdits()
            val layer = prevState.oldState.featureForm.feature.featureTable?.layer as FeatureLayer
            layer.clearSelection()
            layer.selectFeature(prevState.newFeature)
            uiState.value =
                UIState.Editing(
                    featureForm = FeatureForm(
                        prevState.newFeature,
                        layer.featureFormDefinition!!
                    )
                )
        }

    fun continueEditing() =
        (uiState.value as? UIState.Switching)?.let { prevState ->
            uiState.value = prevState.oldState
        }

    fun rollbackEdits(): Result<Unit> {
        (uiState.value as? UIState.Editing)?.let {
            it.featureForm.discardEdits()
            // unselect the feature
            (it.featureForm.feature.featureTable?.layer as FeatureLayer).clearSelection()
            uiState.value = UIState.NotEditing
            return Result.success(Unit)
        } ?: return Result.failure(IllegalStateException("Not in editing state"))
    }

    fun onSingleTapConfirmed(singleTapEvent: SingleTapConfirmedEvent) {
        coroutineScope.launch {
            mapViewProxy.identifyLayers(
                screenCoordinate = singleTapEvent.screenCoordinate,
                tolerance = 22.dp,
                returnPopupsOnly = false
            ).onSuccess { results ->
                try {
                    results.forEach { result ->
                        result.geoElements.firstOrNull {
                            it is ArcGISFeature && (it.featureTable?.layer as? FeatureLayer)?.featureFormDefinition != null
                        }?.let {
                            Log.d("MapScreen", "onSingleTapConfirmed - onSuccess: ${uiState.value}")
                            if (uiState.value is UIState.Editing) {
                                val currentState = uiState.value as UIState.Editing
                                val newFeature = it as ArcGISFeature
                                uiState.value = UIState.Switching(
                                    oldState = currentState,
                                    newFeature = newFeature
                                )
                            } else if (uiState.value is UIState.NotEditing) {
                                val feature = it as ArcGISFeature
                                val layer = feature.featureTable!!.layer as FeatureLayer
                                val featureForm =
                                    FeatureForm(feature, layer.featureFormDefinition!!)
                                // select the feature
                                layer.selectFeature(feature)
                                // set the UI to an editing state with the FeatureForm
                                uiState.value = UIState.Editing(featureForm)
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            getApplication<Application>().applicationContext,
                            "failed to create a FeatureForm for the feature",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun setDefaultState() {
        uiState.value = UIState.NotEditing
    }

    fun onMapLoaded() {
        Log.d("MapViewModel", "Map loaded successfully")
        uiState.value = UIState.NotEditing
    }
}

/**
 * Returns the [FieldFormElement] with the given [fieldName] in the [FeatureForm]. If none exists
 * null is returned.
 */
fun List<FormElement>.getFormElement(fieldName: String): FieldFormElement? {
    val fieldElements = filterIsInstance<FieldFormElement>()
    val element = if (fieldElements.isNotEmpty()) {
        fieldElements.firstNotNullOfOrNull {
            if (it.fieldName == fieldName) it else null
        }
    } else {
        null
    }

    return element ?: run {
        val groupElements = filterIsInstance<GroupFormElement>()
        if (groupElements.isNotEmpty()) {
            groupElements.firstNotNullOfOrNull {
                it.elements.getFormElement(fieldName)
            }
        } else {
            null
        }
    }
}


