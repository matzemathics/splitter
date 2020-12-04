cmake_minimum_required(VERSION 3.4.1)

add_library( opus

             SHARED

             ./src/main/cpp/opus_encoder.c
             ./src/main/cpp/celt/celt_encoder.c
             ./src/main/cpp/celt/bands.c
             ./src/main/cpp/celt/entcode.c
             ./src/main/cpp/celt/entdec.c
             ./src/main/cpp/celt/entenc.c
             ./src/main/cpp/celt/mathops.c
             ./src/main/cpp/celt/vq.c
             ./src/main/cpp/celt/cwrs.c
             ./src/main/cpp/celt/celt.c
             ./src/main/cpp/celt/mdct.c
             ./src/main/cpp/celt/kiss_fft.c
             ./src/main/cpp/celt/bands.c
             ./src/main/cpp/celt/pitch.c
             ./src/main/cpp/celt/celt_lpc.c
             ./src/main/cpp/celt/quant_bands.c
             ./src/main/cpp/celt/laplace.c
             ./src/main/cpp/celt/modes.c
             ./src/main/cpp/celt/rate.c
             ./src/main/cpp/silk/lin2log.c
             ./src/main/cpp/silk/enc_API.c
             ./src/main/cpp/silk/resampler.c
             ./src/main/cpp/silk/resampler_private_IIR_FIR.c
             ./src/main/cpp/silk/resampler_private_up2_HQ.c
             ./src/main/cpp/silk/resampler_private_down_FIR.c
             ./src/main/cpp/silk/resampler_private_AR2.c
             ./src/main/cpp/silk/resampler_rom.c
             ./src/main/cpp/silk/float/encode_frame_FLP.c
             ./src/main/cpp/silk/gain_quant.c
             ./src/main/cpp/silk/log2lin.c
             ./src/main/cpp/silk/encode_pulses.c
             ./src/main/cpp/silk/code_signs.c
             ./src/main/cpp/silk/tables_pulses_per_block.c
             ./src/main/cpp/silk/tables_other.c
             ./src/main/cpp/silk/shell_coder.c
             ./src/main/cpp/silk/encode_indices.c
             ./src/main/cpp/silk/tables_LTP.c
             ./src/main/cpp/silk/tables_pitch_lag.c
             ./src/main/cpp/silk/NLSF_unpack.c
             ./src/main/cpp/silk/tables_gain.c
             ./src/main/cpp/silk/float/wrappers_FLP.c
             ./src/main/cpp/silk/quant_LTP_gains.c
             ./src/main/cpp/silk/VQ_WMat_EC.c
             ./src/main/cpp/silk/NSQ.c
             ./src/main/cpp/silk/LPC_analysis_filter.c
             ./src/main/cpp/silk/NSQ_del_dec.c
             ./src/main/cpp/silk/process_NLSFs.c
             ./src/main/cpp/silk/NLSF2A.c
             ./src/main/cpp/silk/bwexpander_32.c
             ./src/main/cpp/silk/LPC_inv_pred_gain.c
             ./src/main/cpp/silk/table_LSF_cos.c
             ./src/main/cpp/silk/NLSF_encode.c
             ./src/main/cpp/silk/NLSF_decode.c
             ./src/main/cpp/silk/NLSF_stabilize.c
             ./src/main/cpp/silk/sort.c
             ./src/main/cpp/silk/NLSF_VQ_weights_laroia.c
             ./src/main/cpp/silk/NLSF_del_dec_quant.c
             ./src/main/cpp/silk/NLSF_VQ.c
             ./src/main/cpp/silk/interpolate.c
             ./src/main/cpp/silk/float/wrappers_FLP.c
             ./src/main/cpp/silk/A2NLSF.c
             ./src/main/cpp/silk/float/process_gains_FLP.c
             ./src/main/cpp/silk/float/find_pred_coefs_FLP.c
             ./src/main/cpp/silk/float/residual_energy_FLP.c
             ./src/main/cpp/silk/float/energy_FLP.c
             ./src/main/cpp/silk/float/LPC_analysis_filter_FLP.c
             ./src/main/cpp/silk/float/find_LPC_FLP.c
             ./src/main/cpp/silk/float/burg_modified_FLP.c
             ./src/main/cpp/silk/float/inner_product_FLP.c
             ./src/main/cpp/silk/float/scale_copy_vector_FLP.c
             ./src/main/cpp/silk/float/LTP_analysis_filter_FLP.c
             ./src/main/cpp/silk/float/LTP_scale_ctrl_FLP.c
             ./src/main/cpp/silk/float/find_LTP_FLP.c
             ./src/main/cpp/silk/float/scale_vector_FLP.c
             ./src/main/cpp/silk/float/regularize_correlations_FLP.c
             ./src/main/cpp/silk/float/corrMatrix_FLP.c
             ./src/main/cpp/silk/float/noise_shape_analysis_FLP.c
             ./src/main/cpp/silk/float/bwexpander_FLP.c
             ./src/main/cpp/silk/float/LPC_inv_pred_gain_FLP.c
             ./src/main/cpp/silk/float/autocorrelation_FLP.c
             ./src/main/cpp/silk/float/warped_autocorrelation_FLP.c
             ./src/main/cpp/silk/float/apply_sine_window_FLP.c
             ./src/main/cpp/silk/float/find_pitch_lags_FLP.c
             ./src/main/cpp/silk/float/pitch_analysis_core_FLP.c
             ./src/main/cpp/silk/pitch_est_tables.c
             ./src/main/cpp/silk/float/sort_FLP.c
             ./src/main/cpp/silk/resampler_down2.c
             ./src/main/cpp/silk/resampler_down2_3.c
             ./src/main/cpp/silk/float/k2a_FLP.c
             ./src/main/cpp/silk/float/schur_FLP.c
             ./src/main/cpp/silk/LP_variable_cutoff.c
             ./src/main/cpp/silk/biquad_alt.c
             ./src/main/cpp/silk/VAD.c
             ./src/main/cpp/silk/sigm_Q15.c
             ./src/main/cpp/silk/ana_filt_bank_1.c
             ./src/main/cpp/silk/control_SNR.c
             ./src/main/cpp/silk/stereo_encode_pred.c
             ./src/main/cpp/silk/stereo_LR_to_MS.c
             ./src/main/cpp/silk/stereo_quant_pred.c
             ./src/main/cpp/silk/stereo_find_predictor.c
             ./src/main/cpp/silk/inner_prod_aligned.c
             ./src/main/cpp/silk/sum_sqr_shift.c
             ./src/main/cpp/silk/HP_variable_cutoff.c
             ./src/main/cpp/silk/control_codec.c
             ./src/main/cpp/silk/tables_NLSF_CB_NB_MB.c
             ./src/main/cpp/silk/tables_NLSF_CB_WB.c
             ./src/main/cpp/silk/control_audio_bandwidth.c
             ./src/main/cpp/silk/init_encoder.c
             ./src/main/cpp/silk/check_control_input.c
             ./src/main/cpp/analysis.c
             ./src/main/cpp/repacketizer.c
             ./src/main/cpp/opus.c
             ./src/main/cpp/opus_decoder.c
             ./src/main/cpp/celt/celt_decoder.c
             ./src/main/cpp/silk/dec_API.c
             ./src/main/cpp/silk/stereo_MS_to_LR.c
             ./src/main/cpp/silk/decode_frame.c
             ./src/main/cpp/silk/PLC.c
             ./src/main/cpp/silk/bwexpander.c
             ./src/main/cpp/silk/CNG.c
             ./src/main/cpp/silk/decode_core.c
             ./src/main/cpp/silk/decode_parameters.c
             ./src/main/cpp/silk/decode_pitch.c
             ./src/main/cpp/silk/decode_pulses.c
             ./src/main/cpp/silk/decode_indices.c
             ./src/main/cpp/silk/stereo_decode_pred.c
             ./src/main/cpp/silk/decoder_set_fs.c
             ./src/main/cpp/silk/init_decoder.c
             ./src/main/cpp/mlp.c
             ./src/main/cpp/mlp_data.c
             ./src/main/cpp/silk/LPC_fit.c )

include_directories( src/main/cpp src/main/cpp/include src/main/cpp/celt src/main/cpp/silk src/main/cpp/silk/float )

add_definitions( -DOPUS_BUILD -DSTDC_HEADERS -DVAR_ARRAYS )

find_library( log-lib log )

target_link_libraries( opus ${log-lib} )
