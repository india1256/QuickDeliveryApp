package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricScooter
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.delivery.DetailedVehicleFareBreakdown
import com.example.data.model.CustomerPaymentMethod
import com.example.data.model.PackageType
import com.example.data.model.UpiAppOption
import com.example.data.model.VehicleType
import java.util.Locale

private val PrimaryTeal = Color(0xFF008080)
private val DeepTeal = Color(0xFF005F60)
private val BrandOrange = Color(0xFFFF6B35)
private val DeepNavy = Color(0xFF0D1B2A)
private val SlateMuted = Color(0xFF475569)
private val SoftBg = Color(0xFFF4F7F9)
private val VerifiedGreen = Color(0xFF059669)
private val SoftTealSurface = Color(0xFFE6F6F4)
private val SoftOrangeSurface = Color(0xFFFFF4EE)

/**
 * CustomerBookingConfirmationScreen
 *
 * Dedicated, clean booking confirmation screen displaying:
 * 1. Pickup address with house/flat, building, area/street, PIN code, and landmark
 * 2. Drop destination with house/flat, building, area/street, PIN code, and landmark
 * 3. Selected vehicle (Bike -> Scooter -> Auto) with capacity & ETA
 * 4. Parcel details with strict 20 kg max weight verified indicator
 * 5. Complete, itemized ₹ fare breakdown (Base, Distance, Weight, Taxes, Tip, Final Total)
 * 6. High-contrast, prominent "Confirm Booking" button synchronized with the Admin Web backend
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerBookingConfirmationScreen(
    customerName: String,
    customerPhone: String,
    pickupHouseFlat: String,
    pickupBuilding: String,
    pickupArea: String,
    pickupPinCode: String,
    pickupLandmark: String,
    pickupFullAddress: String,
    dropHouseFlat: String,
    dropBuilding: String,
    dropArea: String,
    dropPinCode: String,
    dropLandmark: String,
    dropFullAddress: String,
    selectedVehicle: VehicleType,
    packageWeightKg: String,
    selectedPackageType: PackageType,
    packageDescription: String,
    hasPhotoProof: Boolean,
    distanceKm: Double,
    detailedFare: DetailedVehicleFareBreakdown,
    tipInr: Int,
    selectedPaymentMethod: CustomerPaymentMethod,
    selectedUpiApp: UpiAppOption,
    onBack: () -> Unit,
    onConfirmBooking: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val finalTotalWithTipInr = detailedFare.totalFareInr + tipInr

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("customer_booking_confirmation_screen"),
        containerColor = SoftBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Quick Delivery • Confirm Booking",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Text(
                            text = "FAST. SIMPLE. SECURE.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("booking_confirmation_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Edit",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryTeal)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Final Payable Fare",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateMuted
                            )
                            Text(
                                text = String.format(Locale.US, "₹%.2f", finalTotalWithTipInr),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrange,
                                modifier = Modifier.testTag("confirmation_final_fare_text")
                            )
                        }
                        Surface(
                            color = SoftOrangeSurface,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = when (selectedPaymentMethod) {
                                    CustomerPaymentMethod.CASH_ON_DELIVERY -> "Cash On Delivery"
                                    CustomerPaymentMethod.GATEWAY_UPI -> "UPI: ${selectedUpiApp.displayName}"
                                    CustomerPaymentMethod.GATEWAY_CREDIT_CARD -> "Credit Card"
                                    CustomerPaymentMethod.GATEWAY_DEBIT_CARD -> "Debit Card"
                                    CustomerPaymentMethod.GATEWAY_NET_BANKING -> "Net Banking"
                                    else -> "Direct Rider UPI"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = BrandOrange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // PRIMARY CONFIRM BOOKING BUTTON
                    Button(
                        onClick = onConfirmBooking,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("confirm_booking_button")
                            .testTag("customer_book_order_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = String.format(Locale.US, "Confirm Booking • ₹%.0f", finalTotalWithTipInr),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("confirmation_edit_details_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Edit Booking Details",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Progress / Ready Card
            item(key = "confirmation_banner_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftTealSurface),
                    border = BorderStroke(1.dp, PrimaryTeal.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryTeal,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ready to Dispatch!",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepTeal
                            )
                            Text(
                                text = "Pickup OTP & Delivery OTP will be generated upon confirming.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }

            // 1. ROUTE ADDRESS CARD (Pickup & Drop Details)
            item(key = "confirmation_route_address_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirmation_route_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Route Addresses",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepNavy
                            )
                            Surface(
                                color = SoftTealSurface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f km Trip", distanceKm),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // PICKUP ADDRESS
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryTeal.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Pickup Location",
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "PICKUP LOCATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PrimaryTeal
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Sender",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                Text(
                                    text = pickupFullAddress.ifBlank { "Pickup address specified on map" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepNavy,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                if (pickupHouseFlat.isNotBlank() || pickupBuilding.isNotBlank() || pickupArea.isNotBlank() || pickupPinCode.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            pickupHouseFlat.takeIf { it.isNotBlank() }?.let { "Flat/House: $it" },
                                            pickupBuilding.takeIf { it.isNotBlank() },
                                            pickupArea.takeIf { it.isNotBlank() },
                                            pickupPinCode.takeIf { it.isNotBlank() }?.let { "PIN: $it" }
                                        ).joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateMuted
                                    )
                                }
                                if (pickupLandmark.isNotBlank()) {
                                    Text(
                                        text = "Landmark: $pickupLandmark",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepTeal
                                    )
                                }
                                Text(
                                    text = "Contact: $customerName ($customerPhone)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFEDF2F7),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // DROP DESTINATION
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = CircleShape,
                                color = BrandOrange.copy(alpha = 0.12f),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = "Drop Destination",
                                        tint = BrandOrange,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "DROP DESTINATION",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandOrange
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Receiver",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateMuted
                                    )
                                }
                                Text(
                                    text = dropFullAddress.ifBlank { "Drop destination specified on map" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepNavy,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                if (dropHouseFlat.isNotBlank() || dropBuilding.isNotBlank() || dropArea.isNotBlank() || dropPinCode.isNotBlank()) {
                                    Text(
                                        text = listOfNotNull(
                                            dropHouseFlat.takeIf { it.isNotBlank() }?.let { "Flat/House: $it" },
                                            dropBuilding.takeIf { it.isNotBlank() },
                                            dropArea.takeIf { it.isNotBlank() },
                                            dropPinCode.takeIf { it.isNotBlank() }?.let { "PIN: $it" }
                                        ).joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SlateMuted
                                    )
                                }
                                if (dropLandmark.isNotBlank()) {
                                    Text(
                                        text = "Landmark: $dropLandmark",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BrandOrange
                                    )
                                }
                                Text(
                                    text = "Contact: Receiver (+91 98450 11223)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                        }
                    }
                }
            }

            // 2. SELECTED VEHICLE CARD
            item(key = "confirmation_vehicle_card") {
                val vehicleIcon: ImageVector = when (selectedVehicle) {
                    VehicleType.BIKE -> Icons.Default.TwoWheeler
                    VehicleType.SCOOTER -> Icons.Default.ElectricScooter
                    VehicleType.AUTO -> Icons.Default.DirectionsCar
                    else -> Icons.Default.LocalShipping
                }
                val vehicleName = when (selectedVehicle) {
                    VehicleType.BIKE -> "Quick Delivery Bike"
                    VehicleType.SCOOTER -> "City Scooter"
                    VehicleType.AUTO -> "Cargo Auto"
                    else -> selectedVehicle.title
                }
                val vehicleEta = when (selectedVehicle) {
                    VehicleType.BIKE -> "~15-20 min delivery"
                    VehicleType.SCOOTER -> "~20-25 min delivery"
                    VehicleType.AUTO -> "~25-35 min delivery"
                    else -> "~35-50 min delivery"
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirmation_vehicle_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = PrimaryTeal.copy(alpha = 0.1f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = vehicleIcon,
                                    contentDescription = vehicleName,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = vehicleName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = SoftTealSurface,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SELECTED",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryTeal,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = vehicleEta,
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                            Text(
                                text = "Base Fare ${detailedFare.formattedBaseFare} • Distance ${detailedFare.formattedDistanceCharge}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepTeal
                            )
                        }
                    }
                }
            }

            // 3. PARCEL WEIGHT & SPECIFICATIONS CARD (Max 20 kg validation verified)
            item(key = "confirmation_parcel_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirmation_parcel_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Parcel Specifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                            }
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, VerifiedGreen.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = VerifiedGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Max 20 kg Verified",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VerifiedGreen
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "PARCEL WEIGHT",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateMuted
                                    )
                                    Text(
                                        text = "${packageWeightKg.ifBlank { "1.0" }} kg",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepNavy,
                                        modifier = Modifier.testTag("confirmation_weight_text")
                                    )
                                    Text(
                                        text = "Allowed up to 20.0 kg",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = VerifiedGreen
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftBg,
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = "CATEGORY",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SlateMuted
                                    )
                                    Text(
                                        text = selectedPackageType.name.replace("_", " "),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepNavy,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (hasPhotoProof) "Photo Attached" else "No photo attached",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (hasPhotoProof) PrimaryTeal else SlateMuted
                                    )
                                }
                            }
                        }

                        if (packageDescription.isNotBlank()) {
                            Text(
                                text = "Note: $packageDescription",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }

            // 4. ITEMIZED ₹ FARE BREAKDOWN CARD
            item(key = "confirmation_fare_breakdown_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirmation_fare_breakdown_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Itemized ₹ Fare Breakdown",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepNavy
                            )
                            Surface(
                                color = SoftTealSurface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Synced with Admin",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Base Fare
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Base Fare (Includes initial 1.0 km)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                            Text(
                                text = detailedFare.formattedBaseFare,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }

                        // Distance Charge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format(Locale.US, "Distance Charge (%.1f km trip)", distanceKm),
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                            Text(
                                text = detailedFare.formattedDistanceCharge,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }

                        // Weight Charge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Weight Charge (${packageWeightKg.ifBlank { "1.0" }} kg)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                            Text(
                                text = detailedFare.formattedWeightCharge,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }

                        // Safe Handling & GST
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Safe Handling & 5% GST",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SlateMuted
                            )
                            Text(
                                text = detailedFare.formattedTaxesAndFees,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepNavy
                            )
                        }

                        // Optional Tip
                        if (tipInr > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Rider Appreciation Tip",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PrimaryTeal
                                )
                                Text(
                                    text = "+ ₹$tipInr",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFEDF2F7),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // FINAL ₹ TOTAL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Final Fare",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepNavy
                                )
                                Text(
                                    text = "Inclusive of all taxes & delivery fees",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateMuted
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "₹%.2f", finalTotalWithTipInr),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandOrange
                            )
                        }
                    }
                }
            }

            // 5. SECURITY & ZERO-PARTIALITY GUARANTEE
            item(key = "confirmation_security_card") {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SoftOrangeSurface),
                    border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Zero Partiality Dispatch Guarantee",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = DeepNavy
                            )
                            Text(
                                text = "Your order will be instantly broadcast to the nearest verified fleet rider with GPS live tracking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateMuted
                            )
                        }
                    }
                }
            }
        }
    }
}
