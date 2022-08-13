package com.example.newbiometriclibrary.states

/**
 * [DeviceState] maintains current state of the device.
 *
 * Note: [DeviceState] is critical section. Only one thread can read or
 * write at any given time to maintain consistency during race condition.
 */
class DeviceState {
    var rebootState: RebootState = RebootState.CommandNotIssued
    var kioskLockState: KioskLockState = KioskLockState.Unknown
    var adminLockState: AdminLockState = AdminLockState.Unknown
    var clearKioskPasswordState: ClearKioskPasswordState = ClearKioskPasswordState.CommandNotIssued
    var wipeDataState: WipeDataState = WipeDataState.CommandNotIssued
    var uninstallState: UninstallState = UninstallState.CommandNotIssued
}

sealed class KioskLockState {
    object Locked : KioskLockState()
    object Unlocked : KioskLockState()
    object Unknown: KioskLockState()
}

sealed class AdminLockState {
    object Locked : AdminLockState()
    object Unlocked : AdminLockState()
    object Unknown: AdminLockState()
}

sealed class UninstallState {
    object CommandIssued : UninstallState()
    object CommandNotIssued : UninstallState()
}

sealed class WipeDataState {
    object CommandIssued : WipeDataState()
    object CommandNotIssued : WipeDataState()
}

sealed class ClearKioskPasswordState {
    object CommandIssued : ClearKioskPasswordState()
    object CommandNotIssued : ClearKioskPasswordState()
}

sealed class RebootState {
    object CommandIssued : RebootState()
    object CommandNotIssued : RebootState()
}