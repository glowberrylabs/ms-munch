package com.glowberrylabs.msmunch.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JoystickResolverTest {

    private val radius = 100f

    @Test
    fun `small touches stay in the deadzone`() {
        val resolver = JoystickResolver()
        assertNull(resolver.onDown(27f, 0f, radius))
        assertNull(resolver.onDown(0f, 0f, radius))
    }

    @Test
    fun `touching the base edge selects the dominant axis`() {
        assertEquals(Direction.RIGHT, JoystickResolver().onDown(28f, 0f, radius))
        assertEquals(Direction.LEFT, JoystickResolver().onDown(-35f, 5f, radius))
        assertEquals(Direction.DOWN, JoystickResolver().onDown(10f, 40f, radius))
        assertEquals(Direction.UP, JoystickResolver().onDown(-5f, -40f, radius))
    }

    @Test
    fun `diagonal ties prefer the horizontal axis`() {
        assertEquals(Direction.RIGHT, JoystickResolver().onDown(30f, 30f, radius))
        assertEquals(Direction.LEFT, JoystickResolver().onDown(-30f, 30f, radius))
    }

    @Test
    fun `direction sticks inside the release radius`() {
        val resolver = JoystickResolver()
        assertEquals(Direction.RIGHT, resolver.onDown(40f, 0f, radius))
        assertEquals(Direction.RIGHT, resolver.onMove(10f, 0f, radius))
        assertEquals(Direction.RIGHT, resolver.onMove(0f, 15f, radius))
    }

    @Test
    fun `direction can change above the release radius`() {
        val resolver = JoystickResolver()
        assertEquals(Direction.RIGHT, resolver.onDown(40f, 0f, radius))
        assertEquals(Direction.DOWN, resolver.onMove(0f, 20f, radius))
        assertEquals(Direction.LEFT, resolver.onMove(-30f, 0f, radius))
    }

    @Test
    fun `release keeps the last direction and the next touch must re-engage`() {
        val resolver = JoystickResolver()
        resolver.onDown(-50f, 0f, radius)
        assertEquals(Direction.LEFT, resolver.onUp())
        assertNull(resolver.onDown(10f, 0f, radius))
        assertEquals(Direction.LEFT, resolver.direction)
    }

    @Test
    fun `moving past the engage radius starts steering`() {
        val resolver = JoystickResolver()
        assertNull(resolver.onDown(0f, 0f, radius))
        assertEquals(Direction.UP, resolver.onMove(0f, -40f, radius))
    }

    @Test
    fun `reset clears the stored direction`() {
        val resolver = JoystickResolver()
        resolver.onDown(40f, 0f, radius)
        resolver.reset()
        assertNull(resolver.direction)
    }
}
