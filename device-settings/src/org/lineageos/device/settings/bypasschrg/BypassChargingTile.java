/*
 * SPDX-FileCopyrightText: 2025 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.device.settings.bypasschrg;

import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import org.lineageos.device.settings.Constants;
import org.lineageos.device.settings.R;

public class BypassChargingTile extends TileService {


    private BypassChargingController mBypassController;

    @Override
    public void onCreate() {
        super.onCreate();
        mBypassController = BypassChargingController.getInstance(this);
    }

    @Override
    public void onStartListening() {
        mBypassController.setTileRefresh(
                () -> updateTileState(mBypassController.getBypassChargingStatus()));
        updateTileState(mBypassController.getBypassChargingStatus());
    }

    @Override
    public void onStopListening() {
        mBypassController.setTileRefresh(null);
    }

    @Override
    public void onClick() {
        // Toggle from the real state. A cached copy went stale whenever bypass was
        // changed elsewhere or the service was rebound, and taps were then dropped.
        if (mBypassController.getBypassChargingStatus() != Constants.BYPASS_OFF) {
            mBypassController.disableBypassCharging();
        } else {
            mBypassController.enableBypassCharging();
        }
        updateTileState(mBypassController.getBypassChargingStatus());
    }

    private void updateTileState(int status) {
        Tile tile = getQsTile();
        if (tile == null) return;

        tile.setState(status==Constants.BYPASS_OFF ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
        tile.setLabel(getString(R.string.bypass_charging_title));
        tile.setContentDescription(getString(R.string.bypass_charging_summary));
        if (status==Constants.BYPASS_WAITING) {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bypass_waiting));
        }
        else {
            tile.setIcon(Icon.createWithResource(this, R.drawable.ic_bypass_charging));
        }
        tile.updateTile();
    }
}
