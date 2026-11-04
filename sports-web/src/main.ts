import { bootstrapApplication } from '@angular/platform-browser';
import { provideZoneChangeDetection } from '@angular/core';
import { provideIonicAngular } from '@ionic/angular';
import { AppComponent } from './app/app.component';
bootstrapApplication(AppComponent, {providers:[provideZoneChangeDetection(), provideIonicAngular({mode:'md'})]}).catch(console.error);
