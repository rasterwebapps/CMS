import { ComponentFixture, TestBed } from '@angular/core/testing';
import { OverlayContainer } from '@angular/cdk/overlay';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

import { ToastHostComponent } from './toast-host.component';
import { ToastService } from './toast.service';

// Regression coverage for the "Confirm ticked substitution(s)" bug (Global Auto-Schedule
// flyout, 2026-09-25): an error toast fired while a CDK-Overlay-based flyout/dialog was
// already open rendered fully formed in the DOM but was never actually visible, because it
// was a plain descendant of `app-root` -- which styles.scss makes a stacking context
// (`position: relative; z-index: 1`) for unrelated layout reasons -- while every flyout/
// dialog portals to `.cdk-overlay-container`, a sibling of `app-root` under `<body>` that
// always paints above it. These specs assert the toast host's own contract (its content
// lives in `.cdk-overlay-container`, not under `app-root`/the fixture's own DOM, and its
// overlay is torn down and freshly recreated each time the toast stack goes from empty to
// non-empty) rather than pixel stacking order, which isn't meaningfully assertable outside
// a real browser compositor -- see that fix's own commit for the manual/Playwright
// verification that this ordering is what actually keeps toasts visible over an open flyout.
describe('ToastHostComponent', () => {
  let fixture: ComponentFixture<ToastHostComponent>;
  let toastService: ToastService;
  let overlayContainer: OverlayContainer;

  function overlayPane(): HTMLElement | null {
    return overlayContainer.getContainerElement().querySelector('.cms-toast-overlay-pane');
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastHostComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(ToastHostComponent);
    toastService = TestBed.inject(ToastService);
    overlayContainer = TestBed.inject(OverlayContainer);
    fixture.detectChanges();
  });

  afterEach(() => {
    toastService.dismissAll();
    overlayContainer.ngOnDestroy();
  });

  it('creates no overlay while there are no toasts', () => {
    expect(overlayPane()).toBeNull();
  });

  it('portals a toast into the CDK overlay container, not into the fixture\'s own DOM', () => {
    toastService.success('Speciality created');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cms-toast')).toBeNull();

    const pane = overlayPane();
    expect(pane).not.toBeNull();
    expect(pane!.textContent).toContain('Speciality created');
  });

  it('disposes the overlay once the last toast is dismissed', () => {
    const id = toastService.success('Speciality created');
    fixture.detectChanges();
    expect(overlayPane()).not.toBeNull();

    toastService.dismiss(id);
    fixture.detectChanges();
    expect(overlayPane()).toBeNull();
  });

  it('recreates a fresh overlay for the next toast after the stack has emptied', () => {
    const firstId = toastService.error('First failure');
    fixture.detectChanges();
    const firstPane = overlayPane();
    expect(firstPane).not.toBeNull();

    toastService.dismiss(firstId);
    fixture.detectChanges();
    expect(overlayPane()).toBeNull();

    toastService.error('Second failure');
    fixture.detectChanges();
    const secondPane = overlayPane();
    expect(secondPane).not.toBeNull();
    expect(secondPane).not.toBe(firstPane);
    expect(secondPane!.textContent).toContain('Second failure');
  });
});
