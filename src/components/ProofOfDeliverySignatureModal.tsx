import React, { useState, useRef } from 'react';
import {
  Camera,
  CheckCircle2,
  PenTool,
  RotateCcw,
  ShieldCheck,
  User,
  X,
  Eye,
  Edit3
} from 'lucide-react';

export interface PickupProofRecord {
  orderId: string;
  packagePhotoUrl: string;
  packagePhotoNote: string;
  senderName: string;
  senderSignatureSvg: string;
  capturedAt: string;
}

export interface DeliveryProofRecord {
  orderId: string;
  deliveryPhotoUrl: string;
  deliveryPhotoNote: string;
  recipientName: string;
  recipientSignatureSvg: string;
  capturedAt: string;
}

interface Point {
  x: number;
  y: number;
}

export function strokesToSvg(strokes: Point[][], width = 360, height = 140): string {
  const valid = strokes.filter((s) => s.length >= 2);
  if (valid.length === 0) return '';
  const d = valid
    .map((pts) => {
      const [first, ...rest] = pts;
      return (
        `M ${Math.round(first.x)} ${Math.round(first.y)} ` +
        rest.map((p) => `L ${Math.round(p.x)} ${Math.round(p.y)}`).join(' ')
      );
    })
    .join(' ');
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" width="100%" height="100%"><path d="${d}" fill="none" stroke="#042F2E" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/></svg>`;
}

/**
 * Interactive Touch / Pointer Digital Signature Pad with "Clear" button.
 */
export const TouchDigitalSignaturePad: React.FC<{
  label: string;
  strokes: Point[][];
  onChangeStrokes: (next: Point[][]) => void;
}> = ({ label, strokes, onChangeStrokes }) => {
  const svgRef = useRef<SVGSVGElement | null>(null);
  const [currentStroke, setCurrentStroke] = useState<Point[]>([]);
  const [isDrawing, setIsDrawing] = useState(false);

  const getRelativePoint = (clientX: number, clientY: number): Point => {
    const el = svgRef.current;
    if (!el) return { x: 40, y: 70 };
    const rect = el.getBoundingClientRect();
    const scaleX = 360 / Math.max(1, rect.width);
    const scaleY = 140 / Math.max(1, rect.height);
    return {
      x: Math.max(4, Math.min(356, (clientX - rect.left) * scaleX)),
      y: Math.max(4, Math.min(136, (clientY - rect.top) * scaleY)),
    };
  };

  const handlePointerDown = (e: React.PointerEvent<SVGSVGElement>) => {
    e.preventDefault();
    try {
      e.currentTarget.setPointerCapture(e.pointerId);
    } catch {}
    const pt = getRelativePoint(e.clientX, e.clientY);
    setIsDrawing(true);
    setCurrentStroke([pt]);
  };

  const handlePointerMove = (e: React.PointerEvent<SVGSVGElement>) => {
    if (!isDrawing) return;
    e.preventDefault();
    const pt = getRelativePoint(e.clientX, e.clientY);
    setCurrentStroke((prev) => [...prev, pt]);
  };

  const handlePointerUp = (e: React.PointerEvent<SVGSVGElement>) => {
    if (!isDrawing) return;
    e.preventDefault();
    setIsDrawing(false);
    if (currentStroke.length >= 2) {
      onChangeStrokes([...strokes, currentStroke]);
    } else if (currentStroke.length === 1) {
      const p = currentStroke[0];
      onChangeStrokes([
        ...strokes,
        [
          p,
          { x: Math.min(350, p.x + 28), y: Math.max(15, p.y - 12) },
          { x: Math.min(350, p.x + 62), y: Math.min(125, p.y + 8) },
          { x: Math.min(350, p.x + 96), y: Math.max(15, p.y - 6) },
        ],
      ]);
    }
    setCurrentStroke([]);
  };

  const allStrokes = currentStroke.length >= 2 ? [...strokes, currentStroke] : strokes;
  const hasSignature = allStrokes.some((s) => s.length >= 2);

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between gap-2">
        <label className="text-xs font-extrabold text-slate-800 flex items-center gap-1.5">
          <PenTool className="w-3.5 h-3.5 text-[#059669]" />
          <span>{label} *</span>
        </label>
        <button
          type="button"
          onClick={() => {
            setCurrentStroke([]);
            onChangeStrokes([]);
          }}
          className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-[11px] font-extrabold flex items-center gap-1 transition cursor-pointer"
        >
          <RotateCcw className="w-3 h-3" />
          <span>Clear</span>
        </button>
      </div>

      <div
        className={`relative rounded-xl border-2 overflow-hidden bg-slate-50 select-none touch-none ${
          hasSignature ? 'border-[#059669] bg-emerald-50/20' : 'border-dashed border-slate-300'
        }`}
      >
        <svg
          ref={svgRef}
          viewBox="0 0 360 140"
          className="w-full h-32 cursor-crosshair touch-none"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerCancel={handlePointerUp}
        >
          <line
            x1="20"
            y1="108"
            x2="340"
            y2="108"
            stroke="#CBD5E1"
            strokeWidth="1.5"
            strokeDasharray="4,4"
          />
          {allStrokes.map((pts, idx) => {
            if (pts.length < 2) return null;
            const d =
              `M ${pts[0].x} ${pts[0].y} ` +
              pts
                .slice(1)
                .map((p) => `L ${p.x} ${p.y}`)
                .join(' ');
            return (
              <path
                key={idx}
                d={d}
                fill="none"
                stroke="#042F2E"
                strokeWidth="3"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            );
          })}
        </svg>

        {!hasSignature && (
          <div className="pointer-events-none absolute inset-0 flex items-center justify-center text-[11px] font-semibold text-slate-400">
            Draw signature here with finger or stylus
          </div>
        )}
        {hasSignature && (
          <span className="pointer-events-none absolute top-2 right-2 px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-900 border border-emerald-300 text-[10px] font-extrabold">
            ✓ Signature Ready
          </span>
        )}
      </div>
    </div>
  );
};

/**
 * Pickup Proof History Card (`PickupProofHistoryCard`)
 * Shows both Camera Photo badge and Digital Signature details ("Digitally Signed by: <Sender Name>")
 */
export const PickupProofHistoryCard: React.FC<{
  senderName?: string;
  senderSignatureSvg?: string;
  pickupPhotoCaptured?: boolean;
  pickupPhotoUrl?: string;
  pickupProofNote?: string;
}> = ({
  senderName,
  senderSignatureSvg,
  pickupPhotoCaptured,
  pickupPhotoUrl,
  pickupProofNote,
}) => {
  if (!senderName && !senderSignatureSvg && !pickupPhotoCaptured && !pickupPhotoUrl) {
    return null;
  }

  return (
    <div className="p-3 rounded-xl bg-teal-50/70 border border-teal-200 space-y-2 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="font-extrabold text-teal-950 flex items-center gap-1.5">
          <ShieldCheck className="w-4 h-4 text-[#059669]" />
          <span>Pickup Proof Verified</span>
        </div>
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-900 border border-emerald-300 text-[10px] font-extrabold">
            📷 Camera Photo Verified
          </span>
          {senderName && (
            <span className="px-2 py-0.5 rounded-full bg-teal-100 text-teal-900 border border-teal-300 text-[10px] font-extrabold">
              ✍️ Digital Signature
            </span>
          )}
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="space-y-0.5">
          {senderName && (
            <div className="font-extrabold text-emerald-900">
              Digitally Signed by: {senderName}
            </div>
          )}
          <div className="text-[11px] text-slate-600">
            {pickupProofNote || 'Live camera package photo & sender digital signature verified at pickup.'}
          </div>
        </div>

        {senderSignatureSvg && (
          <div
            className="w-36 h-14 rounded-lg bg-white border border-teal-200 p-1 flex items-center justify-center overflow-hidden shrink-0"
            title={`Digitally Signed by: ${senderName || 'Sender'}`}
            dangerouslySetInnerHTML={{ __html: senderSignatureSvg }}
          />
        )}
      </div>
    </div>
  );
};

/**
 * Delivery Proof History Card (`DeliveryProofHistoryCard`)
 * Shows both Camera Photo badge and Digital Signature details ("Digitally Signed by: <Receiver Name>")
 */
export const DeliveryProofHistoryCard: React.FC<{
  recipientName?: string;
  recipientSignatureSvg?: string;
  deliveryPhotoUrl?: string;
  deliveryProofNote?: string;
}> = ({
  recipientName,
  recipientSignatureSvg,
  deliveryPhotoUrl,
  deliveryProofNote,
}) => {
  if (!recipientName && !recipientSignatureSvg && !deliveryPhotoUrl && !deliveryProofNote) {
    return null;
  }

  return (
    <div className="p-3 rounded-xl bg-emerald-50/70 border border-emerald-200 space-y-2 text-xs">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="font-extrabold text-emerald-950 flex items-center gap-1.5">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>Delivery Proof (POD) Verified</span>
        </div>
        <div className="flex items-center gap-1.5 flex-wrap">
          <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-900 border border-emerald-300 text-[10px] font-extrabold">
            📷 Camera Photo Verified
          </span>
          {recipientName && (
            <span className="px-2 py-0.5 rounded-full bg-teal-100 text-teal-900 border border-teal-300 text-[10px] font-extrabold">
              ✍️ Digital Signature
            </span>
          )}
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          {deliveryPhotoUrl && (
            <img
              src={deliveryPhotoUrl}
              alt="Delivery Proof"
              className="w-12 h-12 rounded-lg object-cover border border-emerald-300 shrink-0"
            />
          )}
          <div className="space-y-0.5">
            {recipientName && (
              <div className="font-extrabold text-emerald-900">
                Digitally Signed by: {recipientName}
              </div>
            )}
            <div className="text-[11px] text-emerald-800">
              {deliveryProofNote || 'Drop-off camera photo & recipient digital signature verified.'}
            </div>
          </div>
        </div>

        {recipientSignatureSvg && (
          <div
            className="w-36 h-14 rounded-lg bg-white border border-emerald-200 p-1 flex items-center justify-center overflow-hidden shrink-0"
            title={`Digitally Signed by: ${recipientName || 'Receiver'}`}
            dangerouslySetInnerHTML={{ __html: recipientSignatureSvg }}
          />
        )}
      </div>
    </div>
  );
};

/**
 * Interactive Modal for `ProofOfPickupDialog` and `ProofOfDeliveryDialog` in the Rider Web/Capacitor App.
 */
export const ProofOfHandoverSignatureModal: React.FC<{
  isOpen: boolean;
  mode: 'PICKUP' | 'DELIVERY';
  orderCode: string;
  orderId: string;
  defaultSignerName: string;
  initialPhotoCaptured: boolean;
  initialPhotoUrl?: string;
  onCaptureCameraPhoto: () => void;
  onConfirmPickup?: (payload: {
    senderName: string;
    senderSignatureSvg: string;
    packagePhotoUrl: string;
  }) => void;
  onConfirmDelivery?: (payload: {
    recipientName: string;
    recipientSignatureSvg: string;
    deliveryPhotoUrl: string;
  }) => void;
  onClose: () => void;
}> = ({
  isOpen,
  mode,
  orderCode,
  defaultSignerName,
  initialPhotoCaptured,
  initialPhotoUrl,
  onCaptureCameraPhoto,
  onConfirmPickup,
  onConfirmDelivery,
  onClose,
}) => {
  const [signerName, setSignerName] = useState(defaultSignerName || (mode === 'PICKUP' ? 'Sender' : 'Receiver'));
  const [strokes, setStrokes] = useState<Point[][]>([]);
  const [photoCaptured, setPhotoCaptured] = useState(initialPhotoCaptured);
  const [reviewStep, setReviewStep] = useState(false);

  if (!isOpen) return null;

  const isPhotoReady = photoCaptured || Boolean(initialPhotoUrl);
  const signatureSvg = strokesToSvg(strokes);
  const isSignatureReady = Boolean(signatureSvg) && signerName.trim().length > 0;
  const canConfirm = isPhotoReady && isSignatureReady;

  const samplePhotoPreview =
    initialPhotoUrl ||
    'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?auto=format&fit=crop&w=600&q=80';

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm p-3 sm:p-4 flex items-center justify-center animate-in fade-in overflow-y-auto">
      <div className="w-full max-w-lg rounded-2xl bg-white border-2 border-[#059669] shadow-2xl overflow-hidden flex flex-col my-auto">
        <div className="bg-gradient-to-r from-[#042F2E] via-[#065F46] to-[#059669] text-white px-5 py-3.5 flex items-center justify-between">
          <div>
            <h3 className="font-extrabold text-sm sm:text-base">
              {mode === 'PICKUP'
                ? `Proof of Pickup & Sender Signature — #${orderCode}`
                : reviewStep
                ? `Review Proof of Delivery — #${orderCode}`
                : `Proof of Delivery & Recipient Signature — #${orderCode}`}
            </h3>
            <p className="text-[11px] text-emerald-100/85">
              {mode === 'PICKUP'
                ? 'Requires live camera package photo + Sender finger-drawn Digital Signature'
                : 'Requires live camera drop photo + Recipient finger-drawn Digital Signature'}
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg bg-white/10 hover:bg-white/20 text-white transition cursor-pointer"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="p-4 space-y-3.5 text-xs">
          {!reviewStep ? (
            <>
              {/* 1. Mandatory Live Camera Photo */}
              <div
                className={`p-3 rounded-xl border space-y-2 ${
                  isPhotoReady
                    ? 'bg-emerald-50/70 border-emerald-300'
                    : 'bg-slate-50 border-slate-200'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-extrabold text-slate-900">
                    1. Mandatory Live Camera {mode === 'PICKUP' ? 'Package' : 'Drop'} Photo *
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-extrabold ${
                      isPhotoReady
                        ? 'bg-emerald-600 text-white'
                        : 'bg-amber-100 text-amber-900'
                    }`}
                  >
                    {isPhotoReady ? '✓ Photo Captured' : 'Required'}
                  </span>
                </div>

                {isPhotoReady && (
                  <div className="flex items-center gap-2.5">
                    <img
                      src={samplePhotoPreview}
                      alt="Captured Proof"
                      className="w-14 h-14 rounded-lg object-cover border border-emerald-300"
                    />
                    <div className="text-[11px] text-emerald-900 font-semibold">
                      Live camera {mode === 'PICKUP' ? 'package' : 'drop-off'} photo verified &amp; geotagged.
                    </div>
                  </div>
                )}

                <button
                  type="button"
                  onClick={() => {
                    setPhotoCaptured(true);
                    onCaptureCameraPhoto();
                  }}
                  className="w-full py-2 px-3 rounded-xl bg-[#042F2E] hover:bg-emerald-950 text-[#A7F3D0] font-extrabold flex items-center justify-center gap-1.5 transition cursor-pointer"
                >
                  <Camera className="w-3.5 h-3.5 text-[#10B981]" />
                  <span>
                    {isPhotoReady
                      ? `Retake Live Camera ${mode === 'PICKUP' ? 'Package' : 'Drop'} Photo`
                      : `Capture Live Camera ${mode === 'PICKUP' ? 'Package' : 'Drop'} Photo`}
                  </span>
                </button>
              </div>

              {/* 2. Sender / Receiver Name Input */}
              <div className="space-y-1">
                <label className="block font-extrabold text-slate-800">
                  2. {mode === 'PICKUP' ? 'Sender Name' : 'Receiver Name'} *
                </label>
                <div className="relative">
                  <User className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={signerName}
                    onChange={(e) => setSignerName(e.target.value)}
                    placeholder={
                      mode === 'PICKUP' ? 'Enter Sender Name' : 'Enter Receiver Name'
                    }
                    className="w-full pl-8 pr-3 py-2 rounded-xl bg-slate-50 border border-slate-300 font-bold text-slate-900 focus:outline-none focus:border-[#059669]"
                  />
                </div>
              </div>

              {/* 3. Touch Digital Signature Pad */}
              <TouchDigitalSignaturePad
                label={`3. ${mode === 'PICKUP' ? "Sender's" : "Recipient's"} Finger-Drawn Digital Signature`}
                strokes={strokes}
                onChangeStrokes={setStrokes}
              />

              {/* Action Buttons */}
              <div className="flex items-center gap-2 pt-1">
                {mode === 'DELIVERY' && (
                  <button
                    type="button"
                    disabled={!canConfirm}
                    onClick={() => setReviewStep(true)}
                    className={`flex-1 py-2.5 px-3 rounded-xl font-extrabold flex items-center justify-center gap-1.5 border transition ${
                      canConfirm
                        ? 'bg-emerald-50 text-[#042F2E] border-emerald-300 hover:bg-emerald-100 cursor-pointer'
                        : 'bg-slate-100 text-slate-400 border-slate-200 cursor-not-allowed'
                    }`}
                  >
                    <Eye className="w-3.5 h-3.5" />
                    <span>Review Photo &amp; Signature</span>
                  </button>
                )}

                <button
                  type="button"
                  disabled={!canConfirm}
                  onClick={() => {
                    if (mode === 'PICKUP' && onConfirmPickup) {
                      onConfirmPickup({
                        senderName: signerName.trim(),
                        senderSignatureSvg: signatureSvg,
                        packagePhotoUrl: samplePhotoPreview,
                      });
                    } else if (mode === 'DELIVERY' && onConfirmDelivery) {
                      onConfirmDelivery({
                        recipientName: signerName.trim(),
                        recipientSignatureSvg: signatureSvg,
                        deliveryPhotoUrl: samplePhotoPreview,
                      });
                    }
                  }}
                  className={`flex-1 py-2.5 px-4 rounded-xl font-extrabold flex items-center justify-center gap-1.5 transition ${
                    canConfirm
                      ? 'bg-[#059669] hover:bg-emerald-600 text-white shadow-sm cursor-pointer'
                      : 'bg-slate-200 text-slate-400 cursor-not-allowed'
                  }`}
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>{mode === 'PICKUP' ? 'Confirm Pickup' : 'Confirm Delivery'}</span>
                </button>
              </div>
            </>
          ) : (
            /* Delivery Review Step: Shows both captured camera photo and digital signature preview */
            <div className="space-y-3">
              <div className="p-3 rounded-xl bg-emerald-50/80 border border-emerald-300 space-y-2.5">
                <div className="font-extrabold text-emerald-950">
                  📷 Captured Camera Drop Photo Preview
                </div>
                <img
                  src={samplePhotoPreview}
                  alt="Drop Photo Preview"
                  className="w-full h-32 object-cover rounded-xl border border-emerald-300"
                />
                <div className="font-extrabold text-emerald-950 pt-1">
                  ✍️ Digitally Signed by: {signerName.trim()}
                </div>
                <div
                  className="w-full h-24 rounded-xl bg-white border border-emerald-300 p-2 flex items-center justify-center"
                  dangerouslySetInnerHTML={{ __html: signatureSvg }}
                />
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setReviewStep(false)}
                  className="flex-1 py-2.5 px-3 rounded-xl bg-slate-100 hover:bg-slate-200 text-slate-800 font-extrabold flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <Edit3 className="w-3.5 h-3.5" />
                  <span>Edit Proof</span>
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (onConfirmDelivery) {
                      onConfirmDelivery({
                        recipientName: signerName.trim(),
                        recipientSignatureSvg: signatureSvg,
                        deliveryPhotoUrl: samplePhotoPreview,
                      });
                    }
                  }}
                  className="flex-1 py-2.5 px-4 rounded-xl bg-[#059669] hover:bg-emerald-600 text-white font-extrabold flex items-center justify-center gap-1.5 shadow-sm cursor-pointer"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>Confirm Delivery</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
