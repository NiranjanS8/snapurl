import React from 'react';

const DeleteConfirmModal = ({ open, onClose, onConfirm, loading }) => {
  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-[1200] flex items-center justify-center bg-black/60 px-4 backdrop-blur-[1px]"
      onClick={loading ? undefined : onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="delete-short-link-title"
      aria-describedby="delete-short-link-description"
    >
      <div
        className="w-full max-w-md rounded-2xl bg-[#1e1e1e] p-6 text-white shadow-[0_22px_56px_rgba(0,0,0,0.34)] transition-all duration-200"
        onClick={(event) => event.stopPropagation()}
      >
        <p className="text-xs font-medium uppercase tracking-[0.14em] text-[#B4A5A5]">
          Confirm Delete
        </p>
        <h2
          id="delete-short-link-title"
          className="mt-3 text-2xl font-semibold tracking-[-0.02em] text-white"
        >
          Delete this short link?
        </h2>
        <p
          id="delete-short-link-description"
          className="mt-3 text-sm leading-6 text-[#B4A5A5]"
        >
          This action is irreversible. The short link and its tracked analytics will be removed permanently.
        </p>
        <div className="mt-6 flex items-center justify-end gap-3">
          <button
            type="button"
            onClick={onClose}
            disabled={loading}
            className="rounded-xl bg-[#151515] px-4 py-2.5 text-sm font-medium text-[#B4A5A5] transition-colors duration-150 hover:text-white disabled:cursor-not-allowed disabled:opacity-45"
          >
            Cancel
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className="rounded-xl bg-[#7A1F2E] px-4 py-2.5 text-sm font-medium text-white transition-colors duration-150 hover:bg-[#96263a] disabled:cursor-not-allowed disabled:opacity-45"
          >
            {loading ? 'Deleting...' : 'Delete'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default DeleteConfirmModal;
