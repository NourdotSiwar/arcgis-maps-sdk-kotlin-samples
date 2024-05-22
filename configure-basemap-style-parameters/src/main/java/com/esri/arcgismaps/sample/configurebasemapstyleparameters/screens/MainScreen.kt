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

package com.esri.arcgismaps.sample.configurebasemapstyleparameters.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arcgismaps.toolkit.geoviewcompose.MapView
import com.esri.arcgismaps.sample.configurebasemapstyleparameters.components.MapViewModel
import com.esri.arcgismaps.sample.sampleslib.components.SampleTopAppBar
import kotlinx.coroutines.launch

/**
 * Main screen layout for the sample app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(sampleName: String) {
    // create a ViewModel to handle MapView interactions
    val mapViewModel: MapViewModel = viewModel()
    val createNewBasemapStyleParameters = mapViewModel::createNewBasemapStyleParameters
    val languageStrategyOptions = mapViewModel.languageStrategyOptions
    val languageStrategy = mapViewModel.languageStrategy
    val onLanguageStrategyChange = mapViewModel.onLanguageStrategyChange
    val specificLanguageOptions = mapViewModel.specificLanguageOptions
    val specificLanguage = mapViewModel.specificLanguage
    val onSpecificLanguageChange = mapViewModel.onSpecificLanguageChange

    val bottomSheetScope = rememberCoroutineScope()
    val bottomSheetState = rememberBottomSheetScaffoldState().apply {
        bottomSheetScope.launch {
            bottomSheetState.show()
        }
    }

    Scaffold(
        topBar = { SampleTopAppBar(title = sampleName) },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it),
                contentAlignment = Alignment.Center
            ) {
                MapView(
                    modifier = Modifier
                        .fillMaxSize(),
                    arcGISMap = mapViewModel.map
                )
                if (!bottomSheetState.bottomSheetState.isVisible) {
                    Button(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        onClick = {
                            bottomSheetScope.launch {
                                bottomSheetState.bottomSheetState.expand()
                            }
                        },
                    ) {
                        Text(
                            text = "Show controls"
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .widthIn(0.dp, 380.dp)
                ) {
                    BottomSheetScaffold(
                        scaffoldState = bottomSheetState,
                        sheetContent = {
                            BoxWithConstraints(
                                Modifier
                                    .heightIn(max = 160.dp)
                                    .padding(8.dp)
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Row {
                                    Column {
                                        SetLanguageStrategy(
                                            languageStrategyOptions = languageStrategyOptions,
                                            onLanguageStrategyChange = { languageStrategy ->
                                                createNewBasemapStyleParameters(
                                                    languageStrategy,
                                                    specificLanguage.value
                                                )
                                                onLanguageStrategyChange(languageStrategy)
                                            },
                                            languageStrategy = languageStrategy.value,
                                            enabled = (specificLanguage.value == "None")
                                        )
                                    }
                                    Divider(
                                        color = Color.LightGray,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .fillMaxHeight()
                                            .width(2.dp)

                                    )
                                    Column {
                                        SetSpecificLanguage(
                                            specificLanguageOptions = specificLanguageOptions,
                                            onSpecificLanguageChange = { specificLanguage ->
                                                createNewBasemapStyleParameters(
                                                    languageStrategy.value,
                                                    specificLanguage
                                                )
                                                onSpecificLanguageChange(specificLanguage)
                                            },
                                            specificLanguage = specificLanguage.value
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                    }
                }
            }
        }
    )
}

/**
 * Define the UI radio buttons for selecting language strategy: "Global" or "Local".
 */
@Composable
private fun SetLanguageStrategy(
    languageStrategyOptions: List<String>,
    onLanguageStrategyChange: (String) -> Unit,
    languageStrategy: String,
    enabled: Boolean
) {
    Text(
        text = "Set language strategy:"
    )

    languageStrategyOptions.forEach { text ->
        Row(
            Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .selectable(
                    selected = (text == languageStrategy),
                    onClick = {
                        if (enabled) {
                            onLanguageStrategyChange(text)
                        }
                    }
                )
        ) {
            RadioButton(
                selected = (text == languageStrategy),
                onClick = {
                    if (enabled) {
                        onLanguageStrategyChange(text)
                    }
                },
                enabled = enabled
            )
            Text(
                text = text,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .wrapContentHeight()
                    .wrapContentWidth()
            )
        }
    }
}

/**
 * Define the UI dropdown menu for selecting specific language: "None", "Bulgarian", "Greek" or
 * "Turkish".
 */
@Composable
private fun SetSpecificLanguage(
    specificLanguageOptions: List<String>,
    onSpecificLanguageChange: (String) -> Unit,
    specificLanguage: String
) {
    Text(
        text = "Set specific language:"
    )
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .wrapContentSize(Alignment.TopStart)
            .padding(top = 16.dp)
    ) {
        Text(
            specificLanguage,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = true })
                .border(
                    2.dp,
                    Color.LightGray
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            specificLanguageOptions.forEachIndexed { index, specificLanguage ->
                DropdownMenuItem(
                    text = { Text(specificLanguage) },
                    onClick = {
                        onSpecificLanguageChange(specificLanguage)
                        expanded = false
                    })
                // show a divider between dropdown menu options
                if (index < specificLanguageOptions.lastIndex) {
                    Divider()
                }
            }
        }
    }
}
