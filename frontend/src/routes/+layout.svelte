<script lang="ts">
  import Modal from '$lib/components/Modal.svelte';
  import { modal } from '$lib/services/modal.service';
  import { onDestroy, onMount } from 'svelte';
  import '../app.css';
  
  let { children } = $props();
  let keepaliveInterval: ReturnType<typeof setInterval>;
  
  onMount(() => {
    // Keepalive каждые 13 минут (токен живет 15 минут)
    keepaliveInterval = setInterval(async () => {
      try {
        const response = await fetch('/api/auth/validate', {
          method: 'GET',
          credentials: 'same-origin'
        });
        
        if (response.ok) {
          console.log('🔄 Keepalive: сессия продлена');
        }
      } catch (e) {
        // тихо падаем
      }
    }, 780000); // 13 минут
  });
  
  onDestroy(() => {
    if (keepaliveInterval) clearInterval(keepaliveInterval);
  });
</script>

<svelte:head>
  <!-- <link rel="icon" href={favicon} /> -->
</svelte:head>

<Modal
    isOpen={$modal.isOpen}
    title={$modal.title}
    message={$modal.message}
    type={$modal.type}
    showConfirm={$modal.showConfirm}
    confirmText={$modal.confirmText}
    cancelText={$modal.cancelText}
    onConfirm={() => {
        $modal.onConfirm?.();
        modal.close();
    }}
    onCancel={() => {
        $modal.onCancel?.();
        modal.close();
    }}
    onClose={modal.close}
/>

{@render children()}