package org.eclipse.smarthome.binding.jemmaremotebinding.handler;

import it.ismb.pert.jemma.jemmaDAL.Appliance;
import it.ismb.pert.jemma.jemmaDAL.Jemma;

public interface ApplianceStatusListener
{
	/**
	 * This method is called when a new appliance has been added.
	 *
	 * @param jemma
	 *            The bridge the changed appliance is connected to.
	 * @param appliance
	 *            The newly created appliance.
	 */
	public void onApplianceAdded(Jemma jemma, Appliance appliance);

	/**
	 * This method is called when the appliance has been removed.
	 *
	 * @param jemma
	 *            The bridge the changed appliance is connected to.
	 * @param appliance
	 *            The appliance about to be removed.
	 */
	public void onApplianceRemoved(Jemma jemma, Appliance appliance);

	/**
	 * This method is called whenever the state of the given appliance has
	 * changed. The new state can be obtained by {@link Appliance#getState()}.
	 *
	 * @param jemma
	 *            The bridge the changed appliance is connected to.
	 * @param appliance
	 *            The appliance which received the state update.
	 */
	public void onApplianceStatusChanged(Jemma jemma, Appliance appliance);

}
