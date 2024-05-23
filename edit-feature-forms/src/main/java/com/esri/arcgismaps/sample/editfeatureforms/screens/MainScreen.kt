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

package com.esri.arcgismaps.sample.editfeatureforms.screens

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arcgismaps.ArcGISEnvironment
import com.arcgismaps.mapping.ArcGISMap
import com.arcgismaps.mapping.BasemapStyle
import com.arcgismaps.mapping.PortalItem
import com.arcgismaps.mapping.featureforms.FeatureForm
import com.arcgismaps.portal.Portal
import com.arcgismaps.toolkit.featureforms.FeatureForm
import com.arcgismaps.toolkit.featureforms.ValidationErrorVisibility
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.editfeatureforms.R
import com.esri.arcgismaps.sample.editfeatureforms.components.ErrorInfo
import com.esri.arcgismaps.sample.editfeatureforms.components.MapViewModel
import com.esri.arcgismaps.sample.editfeatureforms.components.UIState
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main screen layout for the sample app
 */
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(sampleName: String) {

    val application = LocalContext.current.applicationContext as Application
    val coroutineScope = rememberCoroutineScope()
    val mapViewModel = remember{ MapViewModel(coroutineScope, application) }
    // control the ModalBottomSheet visibility, so that when its content is hidden
    // the MapView is not blocked by the ModalBottomSheet and can be tapped
    val showModalBottomSheet = remember { mutableStateOf(false) }
    var showDiscardEditsDialog by remember { mutableStateOf(false) }
    val uiState by mapViewModel.uiState
    val (featureForm, errorVisibility) = remember(uiState) {
        Log.d("MapScreen", "uiState changed to: $uiState")
        setFeatureFormAndErrorVisibility(uiState) {
            showModalBottomSheet.value = it
        }
    }

    LaunchedEffect(Unit) {
        mapViewModel.arcGISMap.load().onSuccess {
            mapViewModel.onMapLoaded()
        }
    }

    Scaffold(
        topBar = {
            if (uiState is UIState.Editing) {
                SampleTopAppBar(
                    title = stringResource(R.string.edit_feature),
                    actions = {
                        OnCloseButton {
                            showDiscardEditsDialog = true
                        }
                        OnCommitButton(
                            mapViewModel::commitEdits
                        )
                    }
                )
            } else {
                SampleTopAppBar(title = sampleName)
            }
        },
    ) {
        MapView(
            arcGISMap = mapViewModel.arcGISMap,
            mapViewProxy = mapViewModel.mapViewProxy,
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            onSingleTapConfirmed = { mapViewModel.onSingleTapConfirmed(it) }
        )

        BottomSheetFeatureForm(
            featureForm = featureForm,
            showModalBottomSheet = showModalBottomSheet.value,
            uiState = uiState,
            errorVisibility = errorVisibility
        ) {
            showModalBottomSheet.value = false
        }
    }

    when (uiState) {
        is UIState.Committing -> {
            CommitDialog(
                errors = (uiState as UIState.Committing).errors,
                cancelCommit = mapViewModel::cancelCommit
            )
        }

        is UIState.Switching -> {
            DiscardEditsDialog(
                onConfirm = mapViewModel::selectNewFeature,
                onCancel = mapViewModel::continueEditing
            )
        }

        else -> {
            Log.d("MapScreen", "uiState: $uiState")
        }
    }

    if (showDiscardEditsDialog) {
        DiscardEditsDialog(
            onConfirm = {
                mapViewModel.rollbackEdits()
                showDiscardEditsDialog = false
            },
            onCancel = {
                showDiscardEditsDialog = false
            }
        )
    }
}

@Composable
fun CommitDialog(
    errors: List<ErrorInfo>,
    cancelCommit: () -> Unit
) {

    if (errors.isEmpty()) {
        CircularProgressIndicator(modifier = Modifier.size(50.dp), strokeWidth = 5.dp)
    } else {
        AlertDialog(
            onDismissRequest = cancelCommit,
            confirmButton = {
                Button(onClick = cancelCommit) {
                    Text(text = stringResource(R.string.back))
                }
            },
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.the_form_has_errors),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(errors.count()) { index ->
                        Text(text = "${errors[index].fieldName} : ${errors[index].error}")
                    }
                }
            })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetFeatureForm(
    featureForm: FeatureForm?,
    showModalBottomSheet: Boolean,
    uiState: UIState,
    errorVisibility: ValidationErrorVisibility,
    onDismissRequest: () -> Unit
) {

    Log.d("MapScreen", "BottomSheetFeatureForm: featureForm is null: ${featureForm != null}, showModalBottomSheet: $showModalBottomSheet")
    AnimatedVisibility(
        visible = featureForm != null && showModalBottomSheet,
        enter = slideInVertically { h -> h },
        exit = slideOutVertically { h -> h },
        label = "feature form"
    ) {
        val isSwitching = uiState is UIState.Switching
        // remember the form and update it when a new form is opened
        val rememberedForm = remember(featureForm, isSwitching) {
            featureForm!!
        }
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = rememberModalBottomSheetState()
        ) {
            // set bottom sheet content to the FeatureForm
            FeatureForm(
                featureForm = rememberedForm,
                modifier = Modifier.fillMaxSize(),
                validationErrorVisibility = errorVisibility
            )
        }
    }
}

@Composable
fun OnCloseButton(setShowDiscardEditsDialog: () -> Unit) {
    IconButton(onClick = {
        setShowDiscardEditsDialog()
    }) {
        Icon(Icons.Default.Close, "Close Feature Editor")
    }
}

@Composable
fun OnCommitButton(commitEdits: suspend () -> Result<Unit>) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    IconButton(onClick = {
        coroutineScope.launch {
            commitEdits().onFailure {
                Log.w("Forms", "Applying edits failed : ${it.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Applying edits failed : ${it.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }) {
        Icon(Icons.Default.Check, "Save Feature")
    }
}

@Composable
fun DiscardEditsDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = stringResource(R.string.discard))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        title = {
            Text(text = stringResource(R.string.discard_edits))
        },
        text = {
            Text(text = stringResource(R.string.all_changes_will_be_lost))
        }
    )
}

private fun setFeatureFormAndErrorVisibility(
    uiState: UIState,
    updateShowModalBottomSheet: (Boolean) -> Unit
) = when (uiState) {
        is UIState.Editing -> {
            val state = (uiState as UIState.Editing)
            updateShowModalBottomSheet(true)
            Pair(state.featureForm, state.validationErrorVisibility)
        }

        is UIState.Committing -> {
            Pair(
                (uiState as UIState.Committing).featureForm,
                ValidationErrorVisibility.Automatic
            )
        }

        is UIState.Switching -> {
            val state = uiState as UIState.Switching
            Pair(
                state.oldState.featureForm, state.oldState.validationErrorVisibility
            )
        }

        else -> {
            updateShowModalBottomSheet(false)
            Pair(null, ValidationErrorVisibility.Automatic)
        }
    }
