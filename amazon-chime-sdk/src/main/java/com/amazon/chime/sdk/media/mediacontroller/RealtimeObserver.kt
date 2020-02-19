package com.amazon.chime.sdk.media.mediacontroller

import com.amazon.chime.sdk.media.enums.SignalStrength
import com.amazon.chime.sdk.media.enums.VolumeLevel

/**
 * [[RealtimeObserver]] lets one listen to real time events such a volume or signal strength changes
 */
interface RealtimeObserver {
    /**
     * Handles volume changes for attendees
     *
     * @param attendeeVolumes: Map<String, VolumeLevel> - A map of attendee Ids to volume
     */
    fun onVolumeChange(attendeeVolumes: Map<String, VolumeLevel>)

    /**
     * Handles signal strength changes for attendees
     *
     * @param attendeeVolumes: Map<String, SignalStrength> - A map of attendee Ids to signal strength
     */
    fun onSignalStrengthChange(attendeeSignalStrength: Map<String, SignalStrength>)
}