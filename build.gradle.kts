tasks.register("clean", Delete::class.java) {
    delete(layout.buildDirectory)
}
