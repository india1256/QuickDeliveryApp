import React, { useState, useRef } from 'react';
import {
  X,
  Camera,
  Upload,
  Image as ImageIcon,
  CheckCircle2,
  RefreshCw,
  Sparkles,
  ShieldCheck,
  FileText,
  User,
  Package,
} from 'lucide-react';

export interface ImageUploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onPhotoSelected: (photoUrl: string, note?: string) => void;
  category: 'DELIVERY_PROOF' | 'PROFILE_PHOTO';
  title?: string;
  subtitle?: string;
  initialImageUrl?: string;
}

// Preset samples for fast and reliable 1-tap demo testing
const DELIVERY_PROOF_PRESETS = [
  {
    id: 'doorstep',
    label: 'Front Doorstep',
    url: 'https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=700&auto=format&fit=crop&q=80',
    desc: 'Package placed safely at customer front door',
  },
  {
    id: 'handover',
    label: 'Direct Handover',
    url: 'https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?w=700&auto=format&fit=crop&q=80',
    desc: 'Handed directly to receiver with verified OTP',
  },
  {
    id: 'security_gate',
    label: 'Security / Reception',
    url: 'https://images.unsplash.com/photo-1578575437130-527eed3abbec?w=700&auto=format&fit=crop&q=80',
    desc: 'Handed over to building security / reception desk',
  },
  {
    id: 'box_parcel',
    label: 'Verified Parcel',
    url: 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=700&auto=format&fit=crop&q=80',
    desc: 'Tamper-proof package sealed and intact',
  },
];

const PROFILE_PHOTO_PRESETS = [
  {
    id: 'rider_pro',
    label: 'Rider Partner',
    url: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=500&auto=format&fit=crop&q=80',
    desc: 'Verified Rider Partner profile headshot',
  },
  {
    id: 'customer_pro',
    label: 'Customer Avatar',
    url: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=500&auto=format&fit=crop&q=80',
    desc: 'Clear, smiling professional avatar',
  },
  {
    id: 'partner_id',
    label: 'ID Portrait',
    url: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=500&auto=format&fit=crop&q=80',
    desc: 'Commercial fleet partner verified ID photo',
  },
  {
    id: 'customer_female',
    label: 'Portrait Photo',
    url: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=500&auto=format&fit=crop&q=80',
    desc: 'Active customer verified profile photo',
  },
];

export const ImageUploadModal: React.FC<ImageUploadModalProps> = ({
  isOpen,
  onClose,
  onPhotoSelected,
  category,
  title,
  subtitle,
  initialImageUrl,
}) => {
  const [selectedImage, setSelectedImage] = useState<string | null>(initialImageUrl || null);
  const [deliveryNote, setDeliveryNote] = useState<string>(
    category === 'DELIVERY_PROOF' ? 'Package safely delivered at doorstep. Verified condition.' : ''
  );
  const [isProcessing, setIsProcessing] = useState<boolean>(false);
  const [activeSourceTab, setActiveSourceTab] = useState<'CAMERA_FILE' | 'PRESETS'>('CAMERA_FILE');

  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);

  if (!isOpen) return null;

  // Process and compress image file via HTML5 Canvas
  const processImageFile = (file: File) => {
    setIsProcessing(true);
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          setSelectedImage(e.target?.result as string);
          setIsProcessing(false);
          return;
        }

        // Limit maximum dimension to 1024px for smooth performance and storage
        const maxDim = 1024;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxDim) {
            height = Math.round((height * maxDim) / width);
            width = maxDim;
          }
        } else {
          if (height > maxDim) {
            width = Math.round((width * maxDim) / height);
            height = maxDim;
          }
        }

        canvas.width = width;
        canvas.height = height;

        // Draw image onto canvas
        ctx.drawImage(img, 0, 0, width, height);

        // If delivery proof, optionally add timestamp & watermark
        if (category === 'DELIVERY_PROOF') {
          const timestamp = new Date().toLocaleString('en-IN', {
            dateStyle: 'medium',
            timeStyle: 'short',
          });

          // Semi-transparent bottom banner for timestamp proof
          ctx.fillStyle = 'rgba(0, 0, 0, 0.65)';
          const bannerHeight = Math.max(36, Math.round(height * 0.08));
          ctx.fillRect(0, height - bannerHeight, width, bannerHeight);

          // Timestamp text
          ctx.fillStyle = '#ffffff';
          ctx.font = `bold ${Math.max(14, Math.round(bannerHeight * 0.4))}px sans-serif`;
          ctx.textAlign = 'left';
          ctx.fillText(`📍 QUICK DELIVERY PROOF • ${timestamp}`, 16, height - bannerHeight / 2 + 5);
        }

        const dataUrl = canvas.toDataURL('image/jpeg', 0.85);
        setSelectedImage(dataUrl);
        setIsProcessing(false);
      };
      img.src = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      processImageFile(e.target.files[0]);
    }
  };

  const handleConfirm = () => {
    if (!selectedImage) return;
    onPhotoSelected(selectedImage, deliveryNote.trim() || undefined);
    onClose();
  };

  const isDeliveryProof = category === 'DELIVERY_PROOF';
  const modalTitle = title || (isDeliveryProof ? 'Proof of Delivery Photo' : 'Update Profile Photo');
  const modalSubtitle =
    subtitle ||
    (isDeliveryProof
      ? 'Capture or upload a clear photo of the delivered parcel for drop verification.'
      : 'Upload a clear profile picture for your account.');

  const presets = isDeliveryProof ? DELIVERY_PROOF_PRESETS : PROFILE_PHOTO_PRESETS;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-150">
      <div className="bg-white rounded-3xl shadow-2xl max-w-lg w-full overflow-hidden border border-slate-200 flex flex-col max-h-[92vh]">
        {/* Header */}
        <div className="p-4 sm:p-5 bg-gradient-to-r from-[#042F2E] to-[#064E3B] text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#059669] flex items-center justify-center text-white shadow-md">
              {isDeliveryProof ? <Package className="w-5 h-5" /> : <User className="w-5 h-5" />}
            </div>
            <div>
              <h3 className="text-base font-extrabold">{modalTitle}</h3>
              <p className="text-xs text-emerald-100/90 leading-tight mt-0.5">{modalSubtitle}</p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 text-white flex items-center justify-center transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-4 sm:p-5 overflow-y-auto space-y-4 flex-1">
          {/* Source Tabs */}
          <div className="flex bg-slate-100 p-1 rounded-2xl text-xs font-bold">
            <button
              type="button"
              onClick={() => setActiveSourceTab('CAMERA_FILE')}
              className={`flex-1 py-2 rounded-xl flex items-center justify-center gap-2 transition cursor-pointer ${
                activeSourceTab === 'CAMERA_FILE'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Camera className="w-4 h-4 text-[#059669]" />
              <span>Camera & Device Upload</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveSourceTab('PRESETS')}
              className={`flex-1 py-2 rounded-xl flex items-center justify-center gap-2 transition cursor-pointer ${
                activeSourceTab === 'PRESETS'
                  ? 'bg-white text-slate-900 shadow-xs'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Sparkles className="w-4 h-4 text-amber-500" />
              <span>Quick Presets</span>
            </button>
          </div>

          {/* Active Image Preview Area */}
          {selectedImage ? (
            <div className="space-y-3">
              <div className="relative rounded-2xl overflow-hidden border-2 border-emerald-500 bg-slate-900 shadow-inner max-h-64 sm:max-h-72 flex items-center justify-center">
                <img
                  src={selectedImage}
                  alt="Selected Preview"
                  className={`w-full object-contain ${
                    !isDeliveryProof ? 'max-h-60 rounded-xl' : 'max-h-64'
                  }`}
                />
                <div className="absolute top-2.5 right-2.5 flex items-center gap-1.5 bg-black/70 backdrop-blur-xs text-white px-2.5 py-1 rounded-full text-[11px] font-bold">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                  <span>Photo Ready</span>
                </div>
              </div>

              {/* Action buttons on image */}
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedImage(null)}
                  className="flex-1 py-2 px-3 rounded-xl border border-slate-300 hover:bg-slate-50 text-slate-700 text-xs font-bold flex items-center justify-center gap-1.5 transition"
                >
                  <RefreshCw className="w-3.5 h-3.5 text-slate-500" />
                  <span>Retake / Choose Another</span>
                </button>
              </div>
            </div>
          ) : (
            <>
              {activeSourceTab === 'CAMERA_FILE' ? (
                <div className="space-y-3">
                  {/* Camera Snap Card */}
                  <div
                    onClick={() => cameraInputRef.current?.click()}
                    className="p-5 rounded-2xl border-2 border-dashed border-emerald-400 bg-emerald-50/50 hover:bg-emerald-50 hover:border-emerald-600 flex flex-col items-center justify-center gap-2 text-center cursor-pointer transition group"
                  >
                    <div className="w-12 h-12 rounded-2xl bg-[#059669] text-white flex items-center justify-center shadow group-hover:scale-105 transition">
                      <Camera className="w-6 h-6" />
                    </div>
                    <div>
                      <div className="text-sm font-extrabold text-slate-900">
                        {isDeliveryProof ? 'Take Live Delivery Photo' : 'Take Selfie / Live Camera'}
                      </div>
                      <div className="text-xs text-slate-600">
                        Launches your device camera for instant capture
                      </div>
                    </div>
                  </div>

                  {/* Device File Upload Card */}
                  <div
                    onClick={() => fileInputRef.current?.click()}
                    className="p-4 rounded-2xl border border-slate-200 bg-slate-50 hover:bg-white hover:border-slate-300 flex items-center gap-3 cursor-pointer transition"
                  >
                    <div className="w-10 h-10 rounded-xl bg-slate-200 text-slate-700 flex items-center justify-center">
                      <Upload className="w-5 h-5" />
                    </div>
                    <div className="flex-1 text-left">
                      <div className="text-xs font-bold text-slate-900">Choose from Device Storage</div>
                      <div className="text-[11px] text-slate-500">Supports JPG, PNG, WEBP files</div>
                    </div>
                  </div>
                </div>
              ) : (
                /* Preset Quick Choices */
                <div className="space-y-2">
                  <div className="text-xs font-extrabold text-slate-700 flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                    <span>Select a 1-Tap Realistic Test Image:</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2.5">
                    {presets.map((item) => (
                      <div
                        key={item.id}
                        onClick={() => setSelectedImage(item.url)}
                        className="rounded-2xl border border-slate-200 hover:border-emerald-500 overflow-hidden cursor-pointer group bg-slate-50 hover:bg-emerald-50/30 transition shadow-2xs"
                      >
                        <div className="h-24 overflow-hidden bg-slate-200 relative">
                          <img
                            src={item.url}
                            alt={item.label}
                            className="w-full h-full object-cover group-hover:scale-105 transition duration-200"
                          />
                        </div>
                        <div className="p-2">
                          <div className="text-xs font-bold text-slate-900 truncate">{item.label}</div>
                          <div className="text-[10px] text-slate-500 line-clamp-1">{item.desc}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </>
          )}

          {/* Hidden file inputs for Camera and File selection */}
          <input
            ref={cameraInputRef}
            type="file"
            accept="image/*"
            capture="environment"
            className="hidden"
            onChange={handleFileChange}
          />
          <input
            ref={fileInputRef}
            type="file"
            accept="image/*"
            className="hidden"
            onChange={handleFileChange}
          />

          {/* Optional Delivery Proof Note */}
          {isDeliveryProof && (
            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                <FileText className="w-3.5 h-3.5 text-slate-500" />
                <span>Delivery Proof Note (Optional)</span>
              </label>
              <input
                type="text"
                value={deliveryNote}
                onChange={(e) => setDeliveryNote(e.target.value)}
                placeholder="e.g. Left with guard, delivered to customer"
                className="w-full px-3.5 py-2.5 rounded-xl border border-slate-300 text-xs focus:outline-hidden focus:border-emerald-600 focus:ring-1 focus:ring-emerald-600 bg-white"
              />
            </div>
          )}

          {/* Verification info badge */}
          <div className="p-3 rounded-2xl bg-emerald-50/80 border border-emerald-200 flex items-start gap-2 text-emerald-950 text-xs">
            <ShieldCheck className="w-4 h-4 text-[#059669] shrink-0 mt-0.5" />
            <div>
              <span className="font-extrabold">Instant Cloud & Local Sync: </span>
              {isDeliveryProof
                ? 'Delivery photos are verified, encrypted, and immediately visible on customer and admin audit screens.'
                : 'Your profile avatar is synced across your account, booking receipts, and live tracking.'}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 sm:p-5 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-2.5">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 font-bold text-xs hover:bg-slate-100 transition cursor-pointer"
          >
            Cancel
          </button>
          <button
            type="button"
            disabled={!selectedImage || isProcessing}
            onClick={handleConfirm}
            className={`px-5 py-2.5 rounded-xl text-xs font-extrabold flex items-center gap-2 shadow-md transition cursor-pointer ${
              selectedImage && !isProcessing
                ? 'bg-[#059669] hover:bg-emerald-600 text-white'
                : 'bg-slate-200 text-slate-400 cursor-not-allowed'
            }`}
          >
            <CheckCircle2 className="w-4 h-4" />
            <span>{isDeliveryProof ? 'Attach Proof & Verify' : 'Save Profile Photo'}</span>
          </button>
        </div>
      </div>
    </div>
  );
};
