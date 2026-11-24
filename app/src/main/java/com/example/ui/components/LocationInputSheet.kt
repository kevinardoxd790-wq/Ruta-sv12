package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ElSalvadorPlaces
import com.example.data.local.OfflinePlaceFtsEntity
import com.example.data.local.OfflinePlaceMatch
import com.example.data.local.SavedPlaceEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.AppIcons
import com.example.util.CoordinateParseResult
import com.example.util.CoordinateParser
import com.example.util.GoogleMapsLinkResolver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInputSheet(
    savedPlaces: List<SavedPlaceEntity>,
    query: String,
    offlineSearchResults: List<OfflinePlaceMatch>,
    isIndexingOfflinePlaces: Boolean,
    indexedPlacesCount: Int,
    onQueryChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSelectDestination: (lat: Double, lon: Double, label: String?) -> Unit,
    onSelectOrigin: (lat: Double, lon: Double, label: String?) -> Unit,
    onUseGpsOrigin: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    var parseResult by remember { mutableStateOf<CoordinateParseResult>(CoordinateParseResult.Empty) }

    LaunchedEffect(query) {
        parseResult = CoordinateParser.parse(query)
        if (parseResult !is CoordinateParseResult.Success && query.isNotBlank()) {
            parseResult = GoogleMapsLinkResolver.resolve(query)
        }
    }

    val filteredSaved = remember(query, savedPlaces) {
        val q = OfflinePlaceFtsEntity.normalizeText(query)
        if (q.isEmpty()) savedPlaces.take(5)
        else savedPlaces.filter {
            OfflinePlaceFtsEntity.normalizeText(it.name).contains(q) ||
                OfflinePlaceFtsEntity.normalizeText(it.notes).contains(q) ||
                OfflinePlaceFtsEntity.normalizeText(it.category).contains(q)
        }
    }
    val filteredGazetteer = remember(query) {
        ElSalvadorPlaces.search(query)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¿Adónde vas?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when {
                            isIndexingOfflinePlaces ->
                                "Indexando lugares y calles offline ($indexedPlacesCount)…"
                            indexedPlacesCount > 0 ->
                                "Búsqueda 100% offline • $indexedPlacesCount lugares y calles"
                            else ->
                                "Búsqueda 100% offline de El Salvador"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("close_search_sheet_button")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                }
            }

            if (isIndexingOfflinePlaces) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_input_field"),
                placeholder = {
                    Text("Ej. Torre Futura, Multiplaza, calle, o enlace Maps…")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar texto")
                        }
                    }
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        val text = clipboard.getText()?.text
                        if (!text.isNullOrBlank()) {
                            onQueryChanged(text)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("paste_clipboard_button")
                ) {
                    Icon(AppIcons.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pegar ubicación", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                OutlinedButton(
                    onClick = {
                        onUseGpsOrigin()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("use_gps_origin_button")
                ) {
                    Icon(AppIcons.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Inicio: Mi GPS", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Instant Coordinate Recognition Card
            when (val currentResult = parseResult) {
                is CoordinateParseResult.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentResult.label ?: "Ubicación detectada",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = CoordinateParser.formatCoordinates(
                                            currentResult.latitude,
                                            currentResult.longitude
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            if (!currentResult.isInsideElSalvador) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Aviso: estas coordenadas están fuera de los límites de El Salvador.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        onSelectDestination(
                                            currentResult.latitude,
                                            currentResult.longitude,
                                            currentResult.label
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("go_parsed_coords_button")
                                ) {
                                    Icon(AppIcons.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ir aquí")
                                }
                                OutlinedButton(
                                    onClick = {
                                        onSelectOrigin(
                                            currentResult.latitude,
                                            currentResult.longitude,
                                            currentResult.label
                                        )
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("set_parsed_origin_button")
                                ) {
                                    Text("Usar inicio")
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                is CoordinateParseResult.ShortenedLinkWarning -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Enlace corto detectado (${currentResult.url}). Sin internet no se puede expandir. Pide que te copien las coordenadas numéricas (ej. 13.6929, -89.2182) o compartan el enlace completo de Google Maps.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                else -> Unit
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (filteredSaved.isNotEmpty()) {
                    item {
                        Text(
                            text = "TUS LUGARES GUARDADOS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(filteredSaved, key = { "saved_${it.id}" }) { place ->
                        PlaceSuggestionRow(
                            title = place.name,
                            subtitle = "${place.category} • ${CoordinateParser.formatCoordinates(place.latitude, place.longitude)}",
                            distanceLabel = null,
                            isSaved = true,
                            onClick = {
                                onSelectDestination(place.latitude, place.longitude, place.name)
                            }
                        )
                    }
                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp)) }
                }

                if (query.trim().length >= 2) {
                    item {
                        Text(
                            text = "LUGARES Y CALLES EN EL SALVADOR (OFFLINE)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (offlineSearchResults.isNotEmpty()) {
                        items(
                            items = offlineSearchResults,
                            key = { "fts_${it.name}_${it.latitude}_${it.longitude}_${it.category}" }
                        ) { match ->
                            val distText = match.distanceMeters?.let { "a ${MainViewModel.formatDistance(it)}" }
                            PlaceSuggestionRow(
                                title = match.name,
                                subtitle = "${match.category} • ${match.subtitle}",
                                distanceLabel = distText,
                                isSaved = false,
                                onClick = {
                                    onSelectDestination(match.latitude, match.longitude, match.name)
                                }
                            )
                        }
                    } else if (filteredGazetteer.isNotEmpty()) {
                        items(filteredGazetteer, key = { "sv_${it.id}" }) { place ->
                            PlaceSuggestionRow(
                                title = place.name,
                                subtitle = "${place.type} • ${place.department}",
                                distanceLabel = null,
                                isSaved = false,
                                onClick = {
                                    onSelectDestination(place.latitude, place.longitude, place.name)
                                }
                            )
                        }
                    } else if (parseResult !is CoordinateParseResult.Success) {
                        item {
                            Text(
                                text = if (isIndexingOfflinePlaces) {
                                    "Terminando de indexar el mapa offline… intenta de nuevo en unos segundos."
                                } else {
                                    "No se encontró ese nombre en el mapa offline. Prueba otro nombre de calle, edificio o pega coordenadas."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "MUNICIPIOS, PLAYAS Y PUNTOS DE EL SALVADOR",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredGazetteer, key = { "sv_${it.id}" }) { place ->
                        PlaceSuggestionRow(
                            title = place.name,
                            subtitle = "${place.type} • ${place.department}",
                            distanceLabel = null,
                            isSaved = false,
                            onClick = {
                                onSelectDestination(place.latitude, place.longitude, place.name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceSuggestionRow(
    title: String,
    subtitle: String,
    distanceLabel: String?,
    isSaved: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isSaved) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) AppIcons.Bookmark else Icons.Default.LocationOn,
                contentDescription = null,
                tint = if (isSaved) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (distanceLabel != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = distanceLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
