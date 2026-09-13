package com.vektorgo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vektorgo.app.data.local.entity.PaymentBillingStatus
import com.vektorgo.app.ui.theme.ArcaBlue
import com.vektorgo.app.ui.theme.ErrorRed
import com.vektorgo.app.ui.theme.ErrorRedContainer
import com.vektorgo.app.ui.theme.GeoPrimaryContainerLight
import com.vektorgo.app.ui.theme.MpGreen
import com.vektorgo.app.ui.theme.WarningAmber

@Composable
fun PaymentStatusBadge(status: PaymentBillingStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, borderColor, icon, label) = when (status) {
        PaymentBillingStatus.INVOICED -> StatusBadgeConfig(
            bgColor = Color(0xFFE8F8EE),
            textColor = Color(0xFF0F766E),
            borderColor = Color(0xFFA7F3D0),
            icon = Icons.Default.CheckCircle,
            label = "FACTURADO"
        )
        PaymentBillingStatus.PENDING_BILLING -> StatusBadgeConfig(
            bgColor = Color(0xFFFFFBEB),
            textColor = Color(0xFFB45309),
            borderColor = Color(0xFFFDE68A),
            icon = Icons.Default.HourglassTop,
            label = "PENDIENTE"
        )
        PaymentBillingStatus.ERROR -> StatusBadgeConfig(
            bgColor = Color(0xFFFEF2F2),
            textColor = Color(0xFFB91C1C),
            borderColor = Color(0xFFFECACA),
            icon = Icons.Default.Error,
            label = "ERROR ARCA"
        )
        PaymentBillingStatus.IGNORED -> StatusBadgeConfig(
            bgColor = Color(0xFFF3EDF7),
            textColor = Color(0xFF49454F),
            borderColor = Color(0xFFCAC4D0),
            icon = null,
            label = "OMITIDO"
        )
    }

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun VoucherTypeBadge(cbteTipo: Int, modifier: Modifier = Modifier) {
    val (label, bgColor, textColor, borderColor) = when (cbteTipo) {
        1 -> Quadruple("FACTURA A", Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFFBAE6FD))
        6 -> Quadruple("FACTURA B", Color(0xFFEDE7F6), Color(0xFF4F378B), Color(0xFFD0BCFF))
        11 -> Quadruple("FACTURA C", Color(0xFFFCE7F3), Color(0xFF9D174D), Color(0xFFFBCFE8))
        else -> Quadruple("COMPROBANTE", Color(0xFFF3EDF7), Color(0xFF49454F), Color(0xFFCAC4D0))
    }

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(6.dp))
            .border(1.dp, borderColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp
        )
    }
}

private data class StatusBadgeConfig(
    val bgColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?,
    val label: String
)

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

