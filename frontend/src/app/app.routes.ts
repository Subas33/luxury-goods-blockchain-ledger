import { Routes } from '@angular/router';

import { AssetDetailPage } from './features/asset-detail/asset-detail-page';
import { HomePage } from './features/home/home-page';

export const routes: Routes = [
  {
    path: '',
    component: HomePage
  },
  {
    path: 'assets/:id',
    component: AssetDetailPage
  },
  {
    path: '**',
    redirectTo: ''
  }
];
