val name: String by settings

rootProject.name = name

val localApi = File("../MiniGamesAPI")
if (localApi.isDirectory) {
    includeBuild(localApi) {
        dependencySubstitution {
            substitute(module("ru.joutak:minigamesapi")).using(project(":"))
        }
    }
}
