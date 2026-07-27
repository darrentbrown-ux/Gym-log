package com.gymlog.app.data

/**
 * Curated, alphabetically-sorted lists of common exercises in each category.
 * "Other" is implicit — the UI always offers a free-text escape.
 *
 * [LibraryEntry.name] is used in dropdowns and the encyclopedia screen.
 * [LibraryEntry.howTo] is a short, single-sentence cue for proper form.
 *
 * Images/animations can be added later by adding a `imageUrl` / `animationUrl` field
 * and rendering it inside the encyclopedia list cell.
 */
object ExerciseCatalog {

    data class LibraryEntry(
        val name: String,
        val howTo: String
    )

    /** Top picks for dropdown + encyclopedia. Kept short to fit the dropdown nicely. */
    val LIBRARY_BY_CATEGORY: Map<ExerciseCategory, List<LibraryEntry>> = mapOf(
        ExerciseCategory.WEIGHT_MACHINE to listOf(
            LibraryEntry("Chest Press",   "Sit with back flat against the pad; grip handles at shoulder width and press straight out until arms are extended, then return slowly."),
            LibraryEntry("Deltoid Fly",   "Sit upright, grip the parallel handles, and open the arms out wide like a hug, then bring them back together to flex the chest."),
            LibraryEntry("Hip Abductor",  "Sit on the padded seat, push outside knees outward against the pads, hold, then return under control."),
            LibraryEntry("Hip Adductor",  "Sit upright, place knees inside the padded pads, squeeze inward against resistance, then release."),
            LibraryEntry("Lat Pulldown",  "Sit and brace thighs under the pad; pull the bar down to the upper chest while keeping back straight, then let it up under control."),
            LibraryEntry("Leg Curl",      "Lie face-down on the bench, hook ankles under the padded bar, curl heels toward your glutes, then lower slowly."),
            LibraryEntry("Leg Extension", "Sit on the bench with shins against the padded bar, extend the knees until legs are straight, then lower under control."),
            LibraryEntry("Leg Press",     "Sit on the seat with feet shoulder-width on the platform; release the safety and press until legs are nearly extended, then bend back to start."),
            LibraryEntry("Pec Deck",      "Sit upright with forearms flat against the pads, bring elbows together in front of the chest, then return under control."),
            LibraryEntry("Pull Down",     "Sit at the lat pulldown, grip the bar slightly wider than shoulders, pull it to your upper chest, then let it back up slowly."),
            LibraryEntry("Rowing Machine","Sit with knees slightly bent, hinge forward from the hips, pull the handle into your lower ribs, then return to start."),
            LibraryEntry("Seated Calf Raise","Sit with knees under the pads, balls of the feet on the platform, raise heels as high as possible, then lower fully."),
            LibraryEntry("Shoulder Press","Sit with back against the pad, grip the handles at shoulder height, press overhead until arms extend, then lower."),
            LibraryEntry("Smith Machine", "Stand under the bar at shoulder height, unrack the bar, perform the lift (squat/bench/press) along the fixed bar path, then re-rack."),
            LibraryEntry("Tricep Extension","Sit upright, grip the bar overhead with arms straight, lower it behind the head by bending elbows, then extend back up.")
        ),
        ExerciseCategory.CARDIO to listOf(
            LibraryEntry("Arc Trainer",     "Step on the pedals, hold the handles lightly, and keep a smooth stride; adjust resistance for intensity."),
            LibraryEntry("Elliptical",      "Step on the pedals and push down while pulling the handles; keep a steady, controlled pace."),
            LibraryEntry("Indoor Rower",    "Drive with the legs first, lean the torso slightly, then pull the handle into your lower chest; reverse in one fluid motion."),
            LibraryEntry("Recumbent Bike",  "Sit back with back against the seat, place feet on the pedals, and pedal smoothly at a steady cadence."),
            LibraryEntry("Stair Master",    "Stand upright with hands resting lightly on the rails, let the steps move beneath you; keep knees soft, not locked."),
            LibraryEntry("Stationary Bike", "Sit on the seat with hands on the handlebars, adjust seat height so the knee has a slight bend at full extension, and pedal."),
            LibraryEntry("Treadmill",       "Stand on the sides, clip the safety key, mount, start slow; gradually increase speed and incline for warmup/cooldown.")
        ),
        ExerciseCategory.CALISTHENICS to listOf(
            LibraryEntry("Burpee",          "From standing, drop to a plank, do a push-up, jump the feet back in, stand, then jump up with arms overhead."),
            LibraryEntry("Captain's Chair","Lift yourself on the parallel armrests, keep shoulders down, tuck knees to chest (or hold) without swinging."),
            LibraryEntry("Dip Bar",         "Grip parallel bars, lift body, lower until upper arms are parallel to the floor, then press back up to straight arms."),
            LibraryEntry("Jumping Jacks",   "Start with feet together and hands at sides; jump feet wide while raising arms overhead, then reverse."),
            LibraryEntry("Mountain Climbers","From a plank, drive one knee toward the chest, then switch legs in a quick running motion."),
            LibraryEntry("Plank",           "Support your weight on forearms and toes, keep the body in a straight line, and hold without sagging the hips."),
            LibraryEntry("Pull-up Bar",     "Hang from the bar with hands shoulder-width, pull yourself up until chin clears the bar, then lower with control."),
            LibraryEntry("Push-ups",        "From a plank with hands under shoulders, lower your chest toward the floor, then press back up; keep the core braced."),
            LibraryEntry("Sit-ups",         "Lie on your back with knees bent, hands behind the head, sit all the way up to the knees, then lower under control."),
            LibraryEntry("Squats",          "Stand with feet shoulder-width, sit hips back and down as if into a chair, then drive through heels to stand."),
            LibraryEntry("Wall Sit",        "Slide down a wall until thighs are parallel to the floor, knees over ankles, and hold the position.")
        ),
        ExerciseCategory.FREE_WEIGHTS to listOf(
            LibraryEntry("Arnold Press",     "Start with palms in toward the face at the shoulders, rotate outward and press overhead until arms are straight."),
            LibraryEntry("Bench Press",      "Lie on the bench with feet flat, grip the bar slightly wider than shoulders, lower to the chest, then press up."),
            LibraryEntry("Bicep Curl",       "Stand with back straight, hold dumbbells at your sides with palms forward, curl the weights to shoulders, then lower."),
            LibraryEntry("Deadlift",         "Stand with feet hip-width, hinge at the hips, grip the bar, drive through the floor and stand tall; keep back neutral."),
            LibraryEntry("Front Raise",      "Hold dumbbells in front of thighs, keep arms straight, raise them forward to shoulder height, then lower under control."),
            LibraryEntry("Goblet Squat",     "Hold a dumbbell vertically at chest, squat to a comfortable depth keeping chest up, then stand tall through heels."),
            LibraryEntry("Hammer Curl",      "Hold dumbbells with palms facing each other, curl the weights up without rotating the wrists, then lower."),
            LibraryEntry("Incline Press",    "Set bench to a moderate incline, press dumbbells or bar from chest level until arms extend overhead, then lower slowly."),
            LibraryEntry("Lateral Raise",    "Hold dumbbells at sides, keep a slight bend in the elbows, raise arms out to the sides to shoulder height, then lower."),
            LibraryEntry("Lunge",            "Step one foot forward, lower the back knee toward the floor, then push back to standing; alternate or stay on one side."),
            LibraryEntry("Overhead Press",   "Stand tall with dumbbells or bar at shoulder height, brace the core, press straight overhead, then lower."),
            LibraryEntry("Romanian Deadlift","Hold weights in front of thighs, hinge from hips pushing the seat back, lower until you feel a hamstring stretch, then stand."),
            LibraryEntry("Skull Crusher",    "Lie on a bench with the bar over the chest, bend at the elbows to bring the bar behind the head, then extend arms to start."),
            LibraryEntry("Squat",            "With bar on the upper back, sit hips back and down until thighs are at least parallel, then drive up through the floor."),
            LibraryEntry("Tricep Pushdown",  "Stand at the cable with elbows tucked to the sides, push the bar/rope down until arms are straight, then return slowly.")
        )
    )

    /** Common lookup that drops the `howTo` text — used for the dropdown selector. */
    val COMMON_BY_CATEGORY: Map<ExerciseCategory, List<String>> =
        LIBRARY_BY_CATEGORY.mapValues { (_, entries) -> entries.map { it.name } }

    /** Reverse lookup: returns the how-to text for any exercise name, or null if not in the library. */
    fun howToFor(name: String): String? {
        LIBRARY_BY_CATEGORY.values.forEach { entries ->
            val match = entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (match != null) return match.howTo
        }
        return null
    }

    /**
     * Suggested machine settings per category. The user can always edit these
     * after the exercise is added (each row is just a name; values are entered
     * per-set on the workout screen or as a per-machine default on the routine).
     *
     * The defaults reflect what *most* machines in that category actually have.
     * For example, every Weight Machine has a seat adjustment; not every one has
     * a chest-pad adjustment, so we don't suggest that one by default.
     */
    fun suggestedSettings(category: ExerciseCategory): List<String> = when (category) {
        ExerciseCategory.WEIGHT_MACHINE -> listOf("Seat height", "Arm position")
        // Cardio: Speed + Incline + Duration are the standard machine HUD fields;
        // Distance is appended so the routine-edit "Defaults" panel includes it
        // (the workout screen also reads Distance from this same map per set).
        ExerciseCategory.CARDIO -> listOf("Speed", "Incline", "Duration", "Distance")
        ExerciseCategory.CALISTHENICS -> emptyList()
        ExerciseCategory.FREE_WEIGHTS -> listOf("Arm position")
    }

    /** Whether a category typically involves a *weight* value per set. */
    fun usesWeight(category: ExerciseCategory): Boolean = category != ExerciseCategory.CALISTHENICS

    /** Whether a category typically involves a *duration* (seconds) value per set. */
    fun usesDuration(category: ExerciseCategory): Boolean = category == ExerciseCategory.CARDIO

    /**
     * Setting *names* used as the user-facing "cardio machine" fields.
     * We expose these as constants so the workout screen can read them
     * without depending on the catalog from elsewhere.
     */
    object CardioFieldNames {
        const val SPEED = "Speed"
        const val INCLINE = "Incline"
        const val DURATION = "Duration"
        const val DISTANCE = "Distance"
    }
}
