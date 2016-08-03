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

package fr.s3i.pointeuse.domaine.pointages.operations;

import fr.s3i.pointeuse.domaine.communs.Contexte;
import fr.s3i.pointeuse.domaine.communs.interactors.boundaries.out.OutBoundary;
import fr.s3i.pointeuse.domaine.communs.operations.Operation1;
import fr.s3i.pointeuse.domaine.pointages.entities.Pointage;
import fr.s3i.pointeuse.domaine.pointages.gateways.PointageRepository;
import fr.s3i.pointeuse.domaine.pointages.services.BusPointage;

/**
 * Created by Adrien on 03/08/2016.
 */
public class EnregistrerPointage extends Operation1<Boolean, Pointage> {

    private final BusPointage bus;

    private final PointageRepository repository;

    public EnregistrerPointage(Contexte contexte, OutBoundary out) {
        super(out);
        this.bus = contexte.getService(BusPointage.class);
        this.repository = contexte.getService(PointageRepository.class);
    }

    @Override
    public Boolean executer(Pointage pointage) {
        String erreur = pointage.getErrorMessage();
        if (erreur == null) {
            repository.persister(pointage);
            bus.post(this, BusPointage.RAFRAICHIR);
        }
        else {
            out.onErreur(erreur);
        }
        return erreur != null;
    }

}
