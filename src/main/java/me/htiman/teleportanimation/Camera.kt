package me.htiman.teleportanimation

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import revxrsal.commands.annotation.Command

object Camera {


    object CameraCommand {
        @Command("camera")
        fun camera(sender: Player, location: LocationEnum) {
            Bukkit.getLogger().info("Camera command executed by ${sender.name} for location ${location.name}")
            sender.sendMessage("Starting camera sequence to ${location.name}.")
            try {
                sender.apply {
                    gameMode = GameMode.SPECTATOR
                    MoveCameraTask(this, location.location, sender, TeleportAnimation.instance)
                            .runTaskTimer(TeleportAnimation.instance, 0, 1)

                }
            } catch (e: Exception) {
                Bukkit.getLogger().severe("Error executing camera command: ${e.message}")
                e.printStackTrace()
            }
        }


        /**
         * Represents a Bukkit task for smoothly moving the camera of a player through a sequence of locations.
         *
         * The camera movement sequence is defined by three stages:
         *  - Stage 0: Move the camera to a high point, adjusting the Y-coordinate and pitch.
         *  - Stage 1: Move the camera to the destination X and Z coordinates.
         *  - Stage 2: Move the camera to the destination Y coordinate.
         *
         * The camera movement is achieved through easing functions, providing smooth acceleration and deceleration effects.
         * The task is executed periodically based on the Bukkit scheduler and is responsible for updating the camera's position
         * and orientation.
         *
         * @param player The player whose camera is being moved.
         * @param location The final destination of the camera movement.
         * @param operator The command sender initiating the camera movement.
         */
        class MoveCameraTask(
                private val player: Player,
                private val location: Location,
                private val operator: CommandSender,
                private val plugin: TeleportAnimation // Plugin instance
        ) : BukkitRunnable() {
            private val destination = location
            private val highPoint = 250.0
            private val duration = 80
            private val epsilon = 0.5
            private var progress = 0.0
            private var stage = 0

            override fun run() {
                try {
                    val (curX, curY, curZ, _, curPitch) = player.location
                    val (destX, destY, destZ, _, destPitch) = destination
                    Bukkit.getLogger().info("Camera movement stage $stage for player ${player.name}")

                    when (stage) {
                        0 -> updateCamera(deltaY = highPoint - curY, deltaPitch = destPitch - curPitch)
                        1 -> updateCamera(deltaX = destX - curX, deltaZ = destZ - curZ)
                        2 -> updateCamera(deltaY = destY - curY)
                        else -> {
                            cancel()
                            player.teleport(destination) // Teleport the player
                            operator.sendMessage("Finished camera sequence and teleported to ${destination.blockX}, ${destination.blockY}, ${destination.blockZ}.")
                                    // Set the player's game mode back to survival
                            player.gameMode = GameMode.SURVIVAL
                        }
                    }
                } catch (e: Exception) {
                    Bukkit.getLogger().severe("Error during camera movement: ${e.message}")
                    e.printStackTrace()
                }
            }

            /**
             * Applies the ease-in-out quadratic easing function to the input.
             *
             * The ease-in-out quadratic easing function provides a smooth acceleration and deceleration effect.
             * The function is defined as follows:
             * - If t is less than 0.5, it returns 2 * t^2.
             * - Otherwise, it returns -1 + (4 - 2 * t) * t.
             *
             * @param t The input value in the range [0, 1] representing the progress of the easing.
             * @return The result of applying the ease-in-out quadratic easing function to the input.
             */
            private fun easeInOutQuad(t: Double) = if (t < 0.5) 2 * t * t else -1 + (4 - 2 * t) * t

            /**
             * Updates the camera's position and orientation based on specified delta values.
             *
             * @param deltaX The change in the X-coordinate.
             * @param deltaY The change in the Y-coordinate.
             * @param deltaZ The change in the Z-coordinate.
             * @param deltaYaw The change in the yaw rotation.
             * @param deltaPitch The change in the pitch rotation.
             */
            private fun updateCamera(
                    deltaX: Double = 0.0,
                    deltaY: Double = 0.0,
                    deltaZ: Double = 0.0,
                    deltaYaw: Float = 0F,
                    deltaPitch: Float = 0F
            ) = player.apply {
                teleport(
                        location.add(deltaX.eased(), deltaY.eased(), deltaZ.eased())
                                .addYawPitch(deltaYaw.eased(), deltaPitch.eased())
                )
            }.also { progress() }

            /**
             * Applies easing to the current value using the ease-in-out quadratic function.
             *
             * @return The eased value.
             */
            private fun Double.eased() = this * easeInOutQuad(progress)

            /**
             * Applies easing to the current value using the ease-in-out quadratic function.
             *
             * @return The eased value.
             */
            private fun Float.eased() = (this * easeInOutQuad(progress)).toFloat()

            /**
             * Advances the progress of the camera movement, and triggers the next stage if the progress is complete.
             */
            private fun progress() {
                progress += 1.0 / duration

                if (progress >= 1.0 - epsilon) {
                    progress = 0.0
                    stage++
                }
            }

            /**
             * Adds the specified yaw and pitch values to the location's current yaw and pitch.
             *
             * @param yaw The change in yaw.
             * @param pitch The change in pitch.
             * @return The modified location with the updated yaw and pitch.
             */
            private fun Location.addYawPitch(yaw: Float, pitch: Float) = apply {
                this.yaw += yaw
                this.pitch += pitch
            }
        }
    }

    /**
     * Used for destructing Bukkit Location
     */
    private operator fun Location.component1(): Double = x
    private operator fun Location.component2(): Double = y
    private operator fun Location.component3(): Double = z
    private operator fun Location.component4(): Float = yaw
    private operator fun Location.component5(): Float = pitch

    enum class LocationEnum(val location: Location) {
        SZ1(Location(Bukkit.getWorld("world2"), -1734.5, 113.0, 1425.5, 180F, 90F)),
        SZ2(Location(Bukkit.getWorld("world2"), -734.0, 113.0, 1449.0, 0F, 90F)),
        SZ3(Location(Bukkit.getWorld("world2"), -1697.5, 113.0, 1435.5, 90F, 90F)),
    }
}