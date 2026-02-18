<script lang="ts">
    import type { TsMsuConfig } from '$lib/types';
    
    export let msu: 'msu1' | 'msu2';
    export let config: TsMsuConfig;
    export let onUpdate: (config: TsMsuConfig) => void;
    export let disableVd: boolean = false;

    function handleVdChange(this: any, vdNumber: number, checked: boolean) {
        const updatedConfig = { ...config };
        const vdKey = `vd${vdNumber}` as keyof TsMsuConfig;
        updatedConfig[vdKey] = checked ? 1 : 0;
        
        // Обновляем флаги
        updatedConfig.prVdMsu = hasAnyVd(updatedConfig) ? 1 : 0;
        updatedConfig.prMsu = (hasAnyVd(updatedConfig) || hasAnyIk(updatedConfig)) ? 1 : 0;
        
        onUpdate(updatedConfig);
    }

    function handleIkChange(this: any, ikNumber: number, checked: boolean) {
        const updatedConfig = { ...config };
        const ikKey = `ik${ikNumber}` as keyof TsMsuConfig;
        updatedConfig[ikKey] = checked ? 1 : 0;
        
        // Обновляем флаги
        updatedConfig.prIkMsu = hasAnyIk(updatedConfig) ? 1 : 0;
        updatedConfig.prMsu = (hasAnyVd(updatedConfig) || hasAnyIk(updatedConfig)) ? 1 : 0;
        
        onUpdate(updatedConfig);
    }

    function hasAnyVd(config: TsMsuConfig): boolean {
        return config.vd1 === 1 || config.vd2 === 1 || config.vd3 === 1;
    }

    function hasAnyIk(config: TsMsuConfig): boolean {
        return [config.ik4, config.ik5, config.ik6, config.ik7, config.ik8, config.ik9, config.ik10]
            .some(value => value === 1);
    }
</script>

<div class="ts-config-grid">
    <div class="checkbox-group">
        {#each [1, 2, 3] as vd}
            <label class="checkbox-label">
                <input 
                    type="checkbox"
                    checked={config[`vd${vd}` as keyof TsMsuConfig] === 1}
                    on:change={(e) => handleVdChange(vd, e.target.checked)}
                />
                <span>ВД{vd}</span>
            </label>
        {/each}
    </div>
    
    <div class="checkbox-group">
        {#each [4, 5, 6, 7, 8, 9, 10] as ik}
            <label class="checkbox-label">
                <input 
                    type="checkbox"
                    checked={config[`ik${ik}` as keyof TsMsuConfig] === 1}
                    on:change={(e) => handleIkChange(ik, e.target.checked)}
                />
                <span>ИК{ik}</span>
            </label>
        {/each}
    </div>
</div>

<style>
    /* .ts-config-grid {
        display: grid;
        grid-template-columns: 1fr 2fr;
        gap: 1rem;
    } */

    .checkbox-group {
        display: flex;
        flex-direction: column;
    }

    .checkbox-label {
        display: flex;
        align-items: center;
        gap: 0.3rem;
        cursor: pointer;
        font-size: 0.875rem;
    }
</style>