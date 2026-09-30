package com.zeno.robot.service

/**
 * Coordination mémoire entre FloatingZenoService et VoiceCommandActivity.
 *
 * Les deux composants sont déclarés dans le même processus Android ":voice".
 * Ce drapeau garantit qu'un seul SpeechRecognizer possède le microphone à la fois.
 */
object VoiceSessionCoordinator {
    @Volatile
    var directVoiceActive: Boolean = false
}
