package com.example.data

data class GazetteerPlace(
    val id: String,
    val name: String,
    val department: String,
    val type: String,
    val latitude: Double,
    val longitude: Double
)

object ElSalvadorPlaces {
    const val DEFAULT_CENTER_LAT = 13.7942
    const val DEFAULT_CENTER_LON = -88.8965
    const val SAN_SALVADOR_LAT = 13.6929
    const val SAN_SALVADOR_LON = -89.2182

    val all: List<GazetteerPlace> = listOf(
        // Cabeceras departamentales y principales ciudades
        GazetteerPlace("ss", "San Salvador (Centro)", "San Salvador", "Capital", 13.6929, -89.2182),
        GazetteerPlace("st", "Santa Tecla", "La Libertad", "Cabecera", 13.6769, -89.2797),
        GazetteerPlace("sa", "Santa Ana", "Santa Ana", "Cabecera", 13.9942, -89.5597),
        GazetteerPlace("sm", "San Miguel", "San Miguel", "Cabecera", 13.4834, -88.1833),
        GazetteerPlace("so", "Sonsonate", "Sonsonate", "Cabecera", 13.7189, -89.7242),
        GazetteerPlace("ah", "Ahuachapán", "Ahuachapán", "Cabecera", 13.9214, -89.8450),
        GazetteerPlace("ch", "Chalatenango", "Chalatenango", "Cabecera", 14.0333, -88.9333),
        GazetteerPlace("co", "Cojutepeque", "Cuscatlán", "Cabecera", 13.7167, -88.9333),
        GazetteerPlace("za", "Zacatecoluca", "La Paz", "Cabecera", 13.5000, -88.8667),
        GazetteerPlace("sv", "San Vicente", "San Vicente", "Cabecera", 13.6333, -88.8000),
        GazetteerPlace("us", "Usulután", "Usulután", "Cabecera", 13.3500, -88.4500),
        GazetteerPlace("sf", "San Francisco Gotera", "Morazán", "Cabecera", 13.6947, -88.1069),
        GazetteerPlace("se", "Sensuntepeque", "Cabañas", "Cabecera", 13.8667, -88.6333),
        GazetteerPlace("lu", "La Unión", "La Unión", "Cabecera", 13.3369, -87.8439),

        // Municipios y zonas clave del Área Metropolitana y todo el país
        GazetteerPlace("soy", "Soyapango", "San Salvador", "Ciudad", 13.7102, -89.1399),
        GazetteerPlace("ac", "Antiguo Cuscatlán", "La Libertad", "Ciudad", 13.6649, -89.2532),
        GazetteerPlace("me", "Mejicanos", "San Salvador", "Ciudad", 13.7403, -89.2131),
        GazetteerPlace("ap", "Apopa", "San Salvador", "Ciudad", 13.8072, -89.1792),
        GazetteerPlace("il", "Ilopango", "San Salvador", "Ciudad", 13.7017, -89.1094),
        GazetteerPlace("smar", "San Martín", "San Salvador", "Municipio", 13.7333, -89.0500),
        GazetteerPlace("ag", "Aguilares", "San Salvador", "Municipio", 13.9572, -89.1897),
        GazetteerPlace("qu", "Quezaltepeque", "La Libertad", "Municipio", 13.8311, -89.2722),
        GazetteerPlace("lo", "Lourdes / Colón", "La Libertad", "Municipio", 13.7219, -89.3653),
        GazetteerPlace("sj", "San Juan Opico", "La Libertad", "Municipio", 13.8761, -89.3597),
        GazetteerPlace("met", "Metapán", "Santa Ana", "Municipio", 14.3333, -89.4500),
        GazetteerPlace("chc", "Chalchuapa", "Santa Ana", "Municipio", 13.9867, -89.6811),
        GazetteerPlace("aca", "Acajutla (Puerto)", "Sonsonate", "Puerto", 13.5928, -89.8275),
        GazetteerPlace("iz", "Izalco", "Sonsonate", "Municipio", 13.7447, -89.6731),
        GazetteerPlace("ju", "Juayúa", "Sonsonate", "Ruta de las Flores", 13.8414, -89.7456),
        GazetteerPlace("ata", "Ataco (Concepción de Ataco)", "Ahuachapán", "Ruta de las Flores", 13.8703, -89.8486),
        GazetteerPlace("apa", "Apaneca", "Ahuachapán", "Ruta de las Flores", 13.8586, -89.8050),
        GazetteerPlace("su", "Suchitoto", "Cuscatlán", "Pueblo", 13.9381, -89.0278),
        GazetteerPlace("lp", "La Palma", "Chalatenango", "Montaña", 14.3167, -89.1667),
        GazetteerPlace("ig", "San Ignacio", "Chalatenango", "Montaña", 14.3397, -89.1828),
        GazetteerPlace("ol", "Olocuilta", "La Paz", "Municipio", 13.5697, -89.1172),
        GazetteerPlace("ai", "Aeropuerto Internacional (Comalapa)", "La Paz", "Aeropuerto", 13.4409, -89.0557),
        GazetteerPlace("be", "Berlín", "Usulután", "Municipio", 13.5000, -88.5333),
        GazetteerPlace("al", "Alegría", "Usulután", "Mirador", 13.5056, -88.4917),
        GazetteerPlace("pe", "Perquín", "Morazán", "Montaña", 13.9589, -88.1631),
        GazetteerPlace("sr", "Santa Rosa de Lima", "La Unión", "Ciudad", 13.6247, -87.8936),

        // Playas, volcanes y fronteras populares
        GazetteerPlace("ll", "Puerto de La Libertad", "La Libertad", "Costa", 13.4883, -89.3222),
        GazetteerPlace("tun", "Playa El Tunco", "La Libertad", "Playa", 13.4931, -89.3819),
        GazetteerPlace("zot", "Playa El Zonte", "La Libertad", "Playa", 13.4956, -89.4367),
        GazetteerPlace("cds", "Costa del Sol", "La Paz", "Playa", 13.3411, -89.0247),
        GazetteerPlace("cuc", "Playa El Cuco", "San Miguel", "Playa", 13.1731, -88.1067),
        GazetteerPlace("con", "Volcán de Conchagua", "La Unión", "Mirador", 13.2750, -87.8500),
        GazetteerPlace("coa", "Lago de Coatepeque", "Santa Ana", "Lago", 13.8631, -89.5469),
        GazetteerPlace("cer", "Cerro Verde", "Santa Ana", "Parque", 13.8275, -89.6228),
        GazetteerPlace("boq", "El Boquerón (Volcán de San Salvador)", "San Salvador", "Parque", 13.7369, -89.2869),
        GazetteerPlace("f_ha", "Frontera Las Chinamas", "Ahuachapán", "Frontera", 14.0172, -89.9089),
        GazetteerPlace("f_ha2", "Frontera La Hachadura", "Ahuachapán", "Frontera", 13.8586, -90.0919),
        GazetteerPlace("f_po", "Frontera El Poy", "Chalatenango", "Frontera", 14.4361, -89.1806),
        GazetteerPlace("f_am", "Frontera El Amatillo", "La Unión", "Frontera", 13.6214, -87.7756)
    )

    fun search(query: String): List<GazetteerPlace> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return all.take(14)
        return all.filter {
            it.name.lowercase().contains(q) ||
                it.department.lowercase().contains(q) ||
                it.type.lowercase().contains(q)
        }
    }
}
