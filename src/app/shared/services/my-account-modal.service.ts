import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { UserDisplay } from '../models/user-display.model';

export interface MyAccountModalState {
  visible: boolean;
  user: UserDisplay | null;
  allowSelfEdit: boolean;
}

const initialState: MyAccountModalState = {
  visible: false,
  user: null,
  allowSelfEdit: false,
};

@Injectable({ providedIn: 'root' })
export class MyAccountModalService {
  private readonly _state = new BehaviorSubject<MyAccountModalState>(initialState);
  readonly state$ = this._state.asObservable();

  open(user: UserDisplay, allowSelfEdit: boolean): void {
    this._state.next({ visible: true, user, allowSelfEdit });
  }

  close(): void {
    this._state.next({ ...initialState });
  }

  getSnapshot(): MyAccountModalState {
    return this._state.value;
  }
}
