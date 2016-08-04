/*
 * Oburo.O est un programme destinée à saisir son temps de travail sur un support Android.
 *
 *     This file is part of Oburo.O
 *     Oburo.O is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package fr.s3i.pointeuse.domaine.pointages.interactors.recapitulatif;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fr.s3i.pointeuse.domaine.communs.Contexte;
import fr.s3i.pointeuse.domaine.communs.interactors.Activable;
import fr.s3i.pointeuse.domaine.communs.interactors.RefreshableInteractor;
import fr.s3i.pointeuse.domaine.communs.services.BusService;
import fr.s3i.pointeuse.domaine.communs.services.logger.Log;
import fr.s3i.pointeuse.domaine.pointages.entities.Pointage;
import fr.s3i.pointeuse.domaine.pointages.gateways.PointageRepository;
import fr.s3i.pointeuse.domaine.pointages.interactors.recapitulatif.boundaries.out.model.PointageRecapitulatif;
import fr.s3i.pointeuse.domaine.pointages.interactors.recapitulatif.boundaries.out.model.PointageRecapitulatifFactory;
import fr.s3i.pointeuse.domaine.pointages.interactors.recapitulatif.boundaries.in.RecapIn;
import fr.s3i.pointeuse.domaine.pointages.interactors.recapitulatif.boundaries.out.RecapOut;
import fr.s3i.pointeuse.domaine.pointages.services.BusPointage;
import fr.s3i.pointeuse.domaine.pointages.services.model.PointageWrapperFactory;
import fr.s3i.pointeuse.domaine.pointages.services.model.PointageWrapperListe;
import fr.s3i.pointeuse.domaine.pointages.utils.Periode;

/**
 * Created by Adrien on 19/07/2016.
 */
public class RecapitulatifInteractor extends RefreshableInteractor<RecapOut> implements RecapIn, BusService.Listener, Activable<BusPointage.WakeupRecapitulatifEvent> {

    private final BusPointage bus;

    private final PointageRepository repository;

    private final PointageWrapperFactory pointageWrapperFactory;

    private final PointageRecapitulatifFactory pointageRecapitulatifFactory;

    public RecapitulatifInteractor(Contexte contexte, RecapOut out) {
        super(out);
        this.bus = contexte.getService(BusPointage.class);
        this.repository = contexte.getService(PointageRepository.class);
        this.pointageWrapperFactory = contexte.getService(PointageWrapperFactory.class);
        this.pointageRecapitulatifFactory = contexte.getService(PointageRecapitulatifFactory.class);
    }

    @Override
    public void initialiser() {
        super.initialiser();
        wakeup();
        bus.subscribe(this);
    }

    @Override
    public void close() throws IOException {
        super.close();
        bus.unsuscribe(this);
    }

    @Override
    public boolean onEvent(BusService.Event event) {
        if (event instanceof BusPointage.RefreshRecapitulatifEvent) {
            Log.info(Log.EVENTS, "{0} ({1}) evenement {2} recu de {3}", this.getClass().getSimpleName(), this, event.getType(), event.getOriginator());
            refresh();
        }
        if (event instanceof BusPointage.WakeupRecapitulatifEvent) {
            Log.info(Log.EVENTS, "{0} ({1}) evenement {2} recu de {3}", this.getClass().getSimpleName(), this, event.getType(), event.getOriginator());
            wakeup();
        }
        return true;
    }

    @Override
    public void activer(BusPointage.WakeupRecapitulatifEvent event) {
        initialiser();
    }

    @Override
    protected Delay refresh() {
        Date maintenant = new Date();

        List<Pointage> pointagesJour = repository.recupererEntre(Periode.JOUR.getDebutPeriode(maintenant), Periode.JOUR.getFinPeriode(maintenant));
        List<Pointage> pointagesSema = repository.recupererEntre(Periode.SEMAINE.getDebutPeriode(maintenant), Periode.SEMAINE.getFinPeriode(maintenant));

        PointageWrapperListe pointagesWrapperJour = pointageWrapperFactory.getPointageWrapper(pointagesJour);
        PointageWrapperListe pointagesWrapperSema = pointageWrapperFactory.getPointageWrapper(pointagesSema);

        PointageRecapitulatif pointageRecapitulatif = pointageRecapitulatifFactory.getRecapitulatif(pointagesWrapperJour, pointagesWrapperSema);
        out.onPointageRecapitulatifUpdate(pointageRecapitulatif);

        Log.debug(Log.EVENTS, "{0} ({1}) rafraichissement recapitulatif", this.getClass().getSimpleName(), this);

        // positionne le rafraichissement auto si nécessaire (si il y a du pointage 'en cours')
        if (pointagesWrapperSema.isEnCours() || pointagesWrapperJour.isEnCours()) {
            return new Delay(3, TimeUnit.SECONDS);
        }
        else {
            return Delay.NO_REFRESH;
        }
    }

}
