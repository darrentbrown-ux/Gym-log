package com.gymlog.app.data

/**
 * Curated, alphabetically-sorted lists of common exercises in each category.
 * "Other" is implicit — the UI always offers a free-text escape.
 */
object ExerciseCatalog {

    /** Top picks for selector (kept short to fit the dropdown nicely). */
    val COMMON_BY_CATEGORY: Map<ExerciseCategory, List<String>> = mapOf(
        ExerciseCategory.WEIGHT_MACHINE to listOf(
            "Chest Press",
            "Deltoid Fly",
            "Hip Abductor",
            "Hip Adductor",
            "Lat Pulldown",
            "Leg Curl",
            "Leg Extension",
            "Leg Press",
            "Pec Deck",
            "Pull Down",
            "Rowing Machine",
            "Seated Calf Raise",
            "Shoulder Press",
            "Smith Machine",
            "Tricep Extension"
        ),
        ExerciseCategory.CARDIO to listOf(
            "Arc Trainer",
            "Elliptical",
            "Indoor Rower",
            "Recumbent Bike",
            "Stair Master",
            "Stationary Bike",
            "Treadmill"
        ),
        ExerciseCategory.CALISTHENICS to listOf(
            "Burpee",
            "Captain's Chair",
            "Dip Bar",
            "Jumping Jacks",
            "Mountain Climbers",
            "Plank",
            "Pull-up Bar",
            "Push-ups",
            "Sit-ups",
            "Squats",
            "Wall Sit"
        ),
        ExerciseCategory.FREE_WEIGHTS to listOf(
            "Arnold Press",
            "Bench Press",
            "Bicep Curl",
            "Deadlift",
            "Front Raise",
            "Goblet Squat",
            "Hammer Curl",
            "Incline Press",
            "Lateral Raise",
            "Lunge",
            "Overhead Press",
            "Romanian Deadlift",
            "Skull Crusher",
            "Squat",
            "Tricep Pushdown"
        )
    )

    /** Suggested machine settings per category, populated when the user adds an exercise. */
    fun suggestedSettings(category: ExerciseCategory): List<String> = when (category) {
        ExerciseCategory.CARDIO -> listOf("Speed", "Incline", "Duration")
        ExerciseCategory.CALISTHENICS -> emptyList()
        // Most strength machines have seat / arm / back adjustments — but we don't
        // force them; the user adds what applies.
        else -> emptyList()
    }

    /** Whether a category typically involves a *weight* value per set. */
    fun usesWeight(category: ExerciseCategory): Boolean = category != ExerciseCategory.CALISTHENICS
}
