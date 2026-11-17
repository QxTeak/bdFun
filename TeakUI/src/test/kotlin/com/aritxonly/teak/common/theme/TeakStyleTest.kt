package com.aritxonly.teak.common.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class TeakStyleTest {
    @Test fun persistedStylesKeepTheirDesignAndLayout() {
        val official = TeakStyle.fromStoredValue(0)
        val classic = TeakStyle.fromStoredValue(1)
        val hyper = TeakStyle.fromStoredValue(2)
        assertSame(official.design, hyper.design)
        assertNotEquals(official.layout, hyper.layout)
        assertTrue(hyper.layout.hasBottomSearch)
        assertFalse(official.layout.hasBottomSearch)
        assertTrue(classic.layout.edgeNavigation)
        assertFalse(hyper.layout.edgeNavigation)
        assertSame(TeakStyle.Official, TeakStyle.fromStoredValue(-1))
    }

    @Test fun designPreservesCallerGeometryAndClassicEdgeLayout() {
        val shape = RoundedCornerShape(18.dp)
        assertSame(shape, TeakDesign.Modern.cardShape(shape))
        assertSame(RectangleShape, TeakDesign.Classic.cardShape(shape))
        for (inset in listOf(8.dp, 12.dp, 14.dp, 16.dp)) {
            assertEquals(inset, TeakDesign.Modern.contentInset(inset))
            assertEquals(0.dp, TeakDesign.Classic.contentInset(inset))
        }
        assertEquals(10.dp, TeakDesign.Modern.itemSpacing(10.dp, 8.dp))
        assertEquals(8.dp, TeakDesign.Classic.itemSpacing(10.dp, 8.dp))
        assertEquals(0.dp, TeakDesign.Classic.itemSpacing(10.dp))
        assertEquals(12.dp, TeakDesign.Classic.sectionTitleStart)
        assertEquals(12.dp, TeakDesign.Classic.sectionTitleTop)
        assertEquals(14.dp, TeakDesign.Modern.sectionTitleStart)
        assertEquals(0.dp, TeakDesign.Modern.sectionTitleTop)
    }
}
