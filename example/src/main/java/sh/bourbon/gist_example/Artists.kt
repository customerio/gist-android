package sh.bourbon.gist_example

data class Artist(
    val name: String,
    val discography: Array<String>
)

class ArtistsMock() {
    companion object {
        fun data() : Array<Artist> {
            return arrayOf(
                Artist("Beatles", arrayOf(
                    "Please Please Me",
                    "With The Beatles",
                    "A Hard Day’s Night",
                    "Beatles For Sale",
                    "Help!",
                    "Rubber Soul",
                    "Revolver",
                    "Sgt Pepper’s Lonely Hearts Club Band",
                    "The Beatles",
                    "Yellow Submarine",
                    "Abbey Road",
                    "Let It Be")
                ),
                Artist("The Doors", arrayOf(
                    "The Doors",
                    "Strange Days",
                    "Waiting for the Sun",
                    "The Soft Parade",
                    "Absolutely Live",
                    "Morrison Hotel",
                    "L.A. Woman",
                    "Other Voices",
                    "Full Circle",
                    "An American Prayer")
                )
            )
        }
    }
}