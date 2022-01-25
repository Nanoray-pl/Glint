package pl.nanoray.glint.store

interface Store<T> {
	val value: T
}

interface MutableStore<T>: Store<T> {
	override var value: T
}