import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

// Page publique affichée avant la connexion : identité de l'établissement et accès à l'espace enseignants.
@Component({
  selector: 'app-accueil',
  imports: [RouterLink],
  templateUrl: './accueil.html',
  styleUrl: './accueil.css',
})
export class Accueil {}
