package com.mechanikdash.game

object LevelRepository {
    val levels: List<LevelDefinition> = listOf(
        LevelDefinition(
            nameRes = R.string.level_1_name,
            requiredGrades = 3,
            introRes = R.string.level_1_intro,
            interiorRows = listOf(
                "p..bg..o..e.",
                "..ssb.#....l",
                "..b.oq...n..",
                "....n.......",
                "..g.....c...",
                "...##ss.....",
                "..o...o..q..",
                "....gll....."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_2_name,
            requiredGrades = 6,
            introRes = R.string.level_2_intro,
            interiorRows = listOf(
                "p..o..g..q..",
                ".#..##..#...",
                "....n..o..g.",
                "..##..n.....",
                "..b...##..e.",
                "..q..g..o...",
                "...##..n.g..",
                "...g........"
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_3_name,
            requiredGrades = 9,
            introRes = R.string.level_3_intro,
            interiorRows = listOf(
                "p..g..#b.o..",
                "..##..#..q..",
                "..o..b....#.",
                "....n..##...",
                "..g..b..n...",
                "..##...o....",
                "..q..g...e..",
                "...g..##...."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_4_name,
            requiredGrades = 12,
            introRes = R.string.level_4_intro,
            interiorRows = listOf(
                "p..##..g..o.",
                "..o...##....",
                "..gllq..#..e",
                "..##..n..n..",
                "..b....##...",
                "..g..o....q.",
                "...##.ss.g..",
                "....g......."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_5_name,
            requiredGrades = 15,
            introRes = R.string.level_5_intro,
            interiorRows = listOf(
                "pb.g..o..q..",
                "..##..##....",
                "..g....n..g.",
                "..o.b##....b",
                "..b...##..e.",
                "..q..n..o...",
                "...##..g....",
                "....g......."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_6_name,
            requiredGrades = 18,
            introRes = R.string.level_6_intro,
            interiorRows = listOf(
                "p..n..o..q..",
                "..##..##n...",
                "..g....m....",
                "..o..##.....",
                "..b...##..e.",
                "..q..g..o...",
                "...##..g....",
                "....g..n...."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_7_name,
            requiredGrades = 21,
            introRes = R.string.level_7_intro,
            interiorRows = listOf(
                "p..g..o..q..",
                "..##..##....",
                "..n.ss.n..g.",
                "..o..##.....",
                "..b.n.##..e.",
                "..q..g..o...",
                "...##..n.ll.",
                "....g......."
            )
        ),
        LevelDefinition(
            nameRes = R.string.level_8_name,
            requiredGrades = 24,
            introRes = R.string.level_8_intro,
            interiorRows = listOf(
                "p..g.bo..q..",
                "..##..##....",
                "..g....n..g.",
                "..o..##.....",
                "..b.n.##..d.",
                "..q..n..o...",
                "...##..g....",
                ".n..g......."
            )
        )
    )
}
