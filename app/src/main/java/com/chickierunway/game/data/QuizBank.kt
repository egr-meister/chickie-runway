package com.chickierunway.game.data

/**
 * ============================================================================================
 *  QUIZ CONTENT — farm & chickens, 3 difficulty levels × 10 questions × 4 options.
 *  REPLACEABLE: edit freely. Rules the code relies on:
 *   - each level keeps a stable, unique `id` (it is the key for saved best scores);
 *   - exactly 10 questions per level (QUESTIONS_PER_CATEGORY);
 *   - exactly 4 options per question, one correct (`correctIndex`).
 * ============================================================================================
 */
data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val correctIndex: Int
)

data class QuizCategory(
    val id: String,
    val title: String,
    val tagline: String,
    /** 1 = easy … 3 = hard. */
    val difficulty: Int,
    val questions: List<QuizQuestion>
)

object QuizBank {

    const val QUESTIONS_PER_CATEGORY = 10
    const val OPTIONS_PER_QUESTION = 4

    /** Builds a question with the correct answer placed at [at] (0..3) among the three wrong ones. */
    private fun q(text: String, correct: String, wrong: List<String>, at: Int): QuizQuestion {
        val position = at.coerceIn(0, wrong.size)
        val options = wrong.toMutableList().apply { add(position, correct) }
        return QuizQuestion(text = text, options = options, correctIndex = position)
    }

    val categories: List<QuizCategory> = listOf(
        QuizCategory(
            id = "easy",
            title = "Easy",
            tagline = "Farmyard basics",
            difficulty = 1,
            questions = listOf(
                q("What sound does a hen make?", "Cluck", listOf("Moo", "Oink", "Baa"), 1),
                q("What is a baby chicken called?", "Chick", listOf("Calf", "Kid", "Piglet"), 0),
                q("What does a hen lay?", "Eggs", listOf("Milk", "Wool", "Honey"), 2),
                q("What is a grown-up male chicken called?", "Rooster", listOf("Bull", "Ram", "Drake"), 3),
                q("Which farm animal gives us wool?", "Sheep", listOf("Cow", "Hen", "Horse"), 1),
                q("Where do chickens live on a farm?", "In a coop", listOf("In a stable", "In a sty", "In a kennel"), 0),
                q("What do dairy cows give us?", "Milk", listOf("Eggs", "Honey", "Wool"), 2),
                q("What is a baby cow called?", "A calf", listOf("A foal", "A lamb", "A kid"), 3),
                q("What does a rooster famously do at sunrise?", "Crows", listOf("Quacks", "Barks", "Neighs"), 1),
                q("Which farm animal says \"oink\"?", "Pig", listOf("Goat", "Duck", "Donkey"), 2)
            )
        ),
        QuizCategory(
            id = "medium",
            title = "Medium",
            tagline = "Coop life & crops",
            difficulty = 2,
            questions = listOf(
                q("About how many days does a chicken egg take to hatch?", "21 days", listOf("7 days", "45 days", "90 days"), 2),
                q("What is the red crest on top of a chicken's head called?", "Comb", listOf("Wattle", "Beak", "Spur"), 0),
                q("What do you call a group of chickens?", "A flock", listOf("A herd", "A pack", "A school"), 3),
                q("A young female chicken under one year old is a…", "Pullet", listOf("Cockerel", "Gosling", "Duckling"), 1),
                q("What do chickens swallow to help their gizzard grind food?", "Small stones (grit)", listOf("Feathers", "Seashells only", "Nothing at all"), 0),
                q("What is a baby horse called?", "A foal", listOf("A calf", "A kid", "A cygnet"), 3),
                q("Which grain is a classic chicken feed?", "Corn", listOf("Cocoa", "Coffee", "Cotton"), 2),
                q("Where do chickens like to sleep at night?", "On a roost", listOf("In a burrow", "In the pond", "In a beehive"), 1),
                q("Dried grass stored to feed farm animals is called…", "Hay", listOf("Moss", "Peat", "Bark"), 0),
                q("What is a male duck called?", "A drake", listOf("A gander", "A cob", "A tom"), 3)
            )
        ),
        QuizCategory(
            id = "hard",
            title = "Hard",
            tagline = "For true farm experts",
            difficulty = 3,
            questions = listOf(
                q("Domestic chickens descend mainly from which wild bird?", "Red junglefowl", listOf("Ostrich", "Peacock", "Pheasant"), 2),
                q("What does a chick use to crack its shell from inside?", "An egg tooth", listOf("Its spurs", "Its wattles", "Its comb"), 0),
                q("The fleshy flaps hanging under a chicken's beak are…", "Wattles", listOf("Hackles", "Spurs", "Combs"), 3),
                q("Eggshell colour (white, brown, blue) mostly depends on the hen's…", "Breed", listOf("Diet", "Coop colour", "Weather"), 1),
                q("A hen that wants to sit on eggs until they hatch is called…", "Broody", listOf("Moulting", "Roosting", "Preening"), 2),
                q("Chickens lose and regrow their feathers each year. This is called…", "Moulting", listOf("Brooding", "Nesting", "Dust bathing"), 0),
                q("Which part of the egg feeds the growing chick?", "The yolk", listOf("The air cell", "The shell", "The chalaza"), 3),
                q("Which breed is famous for laying blue-green eggs?", "Araucana", listOf("Leghorn", "Rhode Island Red", "Orpington"), 1),
                q("How do chickens clean their feathers and fight mites?", "Dust bathing", listOf("Swimming", "Rolling in snow", "Standing in rain"), 2),
                q("Small-sized chicken breeds are known as…", "Bantams", listOf("Pullets", "Broilers", "Capons"), 0)
            )
        )
    )

    fun category(id: String?): QuizCategory? =
        if (id.isNullOrBlank()) null else categories.firstOrNull { it.id == id }
}
