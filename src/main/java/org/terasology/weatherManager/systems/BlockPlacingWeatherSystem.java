/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.weatherManager.systems;

import org.terasology.context.Context;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.delay.PeriodicActionTriggeredEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.network.Client;
import org.terasology.network.NetworkSystem;
import org.terasology.registry.In;
import org.terasology.utilities.random.FastRandom;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;

@RegisterSystem(RegisterMode.AUTHORITY)
public class BlockPlacingWeatherSystem extends BaseComponentSystem {
    private static final int SNOW_BLOCK_RANGE = 40;
    private Block air;
    private Block snow;

    @In
    private WorldProvider worldProvider;

    @In
    private BlockManager blockManager;

    @In
    private Context context;

    private NetworkSystem networkSystem;

    @Override
    public void postBegin() {
        air = blockManager.getBlock("engine:air");
        snow = blockManager.getBlock("WeatherManager:snow");
        networkSystem = context.get(NetworkSystem.class);
    }

    /**
     * Places snow blocks on the ground when it is snowing.
     * The event with the id "placeSnow" will only be created when it is snowing.
     * @param event The event that means it is time to place snow
     * @param worldEntity The entity that sent the event (assumed to be the player)
     */
    @ReceiveEvent
    public void onPlaceEvent(PeriodicActionTriggeredEvent event, EntityRef worldEntity) {
        if (event.getActionId().equals("placeSnow")) {
            for(Client currentPlayer : networkSystem.getPlayers()) {
                LocationComponent locComp = currentPlayer.getEntity().getComponent(LocationComponent.class);
                Vector3i playerPos = new Vector3i(locComp.getWorldPosition());

                placeSnow(playerPos);
            }
        } else if (event.getActionId().equals("removeSnow")) {
            for(Client currentPlayer : networkSystem.getPlayers()) {
                LocationComponent locComp = currentPlayer.getEntity().getComponent(LocationComponent.class);
                Vector3i playerPos = new Vector3i(locComp.getWorldPosition());

                removeSnow(playerPos);
            }
        }
    }

    private void placeSnow(Vector3i playerPos) {
        FastRandom rand = new FastRandom();
        int x = (int) playerPos.x + rand.nextInt(SNOW_BLOCK_RANGE * 2) - SNOW_BLOCK_RANGE;
        int z = (int) playerPos.z + rand.nextInt(SNOW_BLOCK_RANGE * 2) - SNOW_BLOCK_RANGE;
        int currentY = (int) playerPos.y + SNOW_BLOCK_RANGE;
        int iter = 0;
        boolean lastGround = false;
        boolean placed = false;
        while (!placed || iter < SNOW_BLOCK_RANGE * 2) {
            Block current = worldProvider.getBlock(x, currentY, z);
            if (current.equals(air) && lastGround) {
                worldProvider.setBlock(new Vector3i(x, currentY, z), snow);
                placed = true;
            } else if (current.equals(air)) {
                currentY--;
                lastGround = false;
            } else if (current.isPenetrable() || !current.isAttachmentAllowed()) {
                break;
            } else if (!current.equals(snow)) {
                lastGround = true;
                currentY++;
            } else {
                placed = true; //break out to avoid double-placing snow
            }
            iter++;
        }
    }

    private void removeSnow(Vector3i playerPos) {
        FastRandom rand = new FastRandom();
        int x = (int) playerPos.x + rand.nextInt(SNOW_BLOCK_RANGE * 2) - SNOW_BLOCK_RANGE;
        int z = (int) playerPos.z + rand.nextInt(SNOW_BLOCK_RANGE * 2) - SNOW_BLOCK_RANGE;
        int currentY = (int) playerPos.y + SNOW_BLOCK_RANGE;
        int iter = 0;
        boolean lastGround = false;
        boolean placed = false;
        while (!placed || iter < SNOW_BLOCK_RANGE * 2) {
            Block current = worldProvider.getBlock(x, currentY, z);
            if (current.equals(snow)) {
                worldProvider.setBlock(new Vector3i(x, currentY, z), air);
                placed = true;
            } else if (lastGround) {
                placed = true; //break out if there is no snow
            } else if (current.equals(air)) {
                currentY--;
                lastGround = false;
            } else {
                lastGround = true;
                currentY++;
            }
            iter++;
        }
    }
}
