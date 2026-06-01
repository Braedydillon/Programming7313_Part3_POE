package Data

data class Expense(
    val category: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val description: String = "",
    val photoUri: String? = null
)
